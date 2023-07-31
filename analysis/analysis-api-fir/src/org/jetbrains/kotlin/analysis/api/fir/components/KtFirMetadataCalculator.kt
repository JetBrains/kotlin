/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.google.common.collect.Multimap
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.util.ClassUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.components.KtMetadataCalculator
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.generateLanguageVersionSettingsBasedMetadataFlags
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.config.JvmAbiStability
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmSerializerExtension
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmSerializerExtension.Companion.FIELD_FOR_PROPERTY
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmSerializerExtension.Companion.METHOD_FOR_FIR_FUNCTION
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.typeApproximator
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.toMetadataVersion
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import java.util.*

@OptIn(KtAnalysisNonPublicApi::class)
internal class KtFirMetadataCalculator(override val analysisSession: KtFirAnalysisSession) : KtMetadataCalculator(),
    KtFirAnalysisSessionComponent {
    private val firSession: FirSession
        get() = rootModuleSession

    private val scopeSession: ScopeSession
        get() = firResolveSession.getScopeSessionFor(firSession)

    private val metadataVersion = firSession.languageVersionSettings.languageVersion.toMetadataVersion()

    override fun calculateMetadata(ktClass: KtClassOrObject, mapping: Multimap<KtElement, PsiElement>): Metadata {
        val firClass = ktClass.getOrBuildFirOfType<FirRegularClass>(firResolveSession)

        val bindings = JvmSerializationBindings().also { collectBindings(firClass.declarations, mapping, it) }
        val (serializer, stringTable) = createTopLevelSerializer(FirMetadataSource.Class(firClass), bindings)
        val classProto = serializer.classProto(firClass)
        return generateAnnotation(classProto.build(), stringTable, KotlinClassHeader.Kind.CLASS)
    }

    override fun calculateMetadata(ktFile: KtFile, mapping: Multimap<KtElement, PsiElement>): Metadata {
        val firFile = ktFile.getOrBuildFirFile(firResolveSession)
        val bindings = JvmSerializationBindings().also { collectBindings(firFile.declarations, mapping, it) }
        val fileList = listOf(firFile)
        val (serializer, stringTable) = createTopLevelSerializer(FirMetadataSource.File(fileList), bindings)
        val fileProto = serializer.packagePartProto(firFile.packageFqName, fileList, null)
        return generateAnnotation(fileProto.build(), stringTable, KotlinClassHeader.Kind.FILE_FACADE)
    }

    private fun collectBindings(
        declarations: List<FirDeclaration>,
        mapping: Multimap<KtElement, PsiElement>,
        bindings: JvmSerializationBindings,
    ) {
        for (fir in declarations) {
            if (fir !is FirFunction && fir !is FirProperty && fir !is FirTypeAlias) continue

            fir.symbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)

            val psiElements = mapping[fir.psi as KtElement]
            val methods = psiElements.filterIsInstance<PsiMethod>()
            when (fir) {
                is FirFunction -> methods.singleOrNull { it.isConstructor == fir is FirConstructor }?.let {
                    bindings.put(
                        METHOD_FOR_FIR_FUNCTION,
                        fir,
                        Method(if (it.isConstructor) SpecialNames.INIT.asString() else it.name, getAsmMethodSignatureWithCorrection(it))
                    )
                }
                is FirProperty -> {
                    if (fir.hasBackingField || fir.delegateFieldSymbol != null) {
                        psiElements.firstIsInstanceOrNull<PsiField>()?.let {
                            bindings.put(FIELD_FOR_PROPERTY, fir, Type.getType(getBinaryPresentationWithCorrection(it.type)) to it.name)
                        }
                    }
                    fir.getter?.let { getter ->
                        methods.singleOrNull { it.returnType != PsiType.VOID }?.let {
                            bindings.put(METHOD_FOR_FIR_FUNCTION, getter, Method(it.name, getAsmMethodSignatureWithCorrection(it)))
                        }
                    }
                    fir.setter?.let { setter ->
                        methods.singleOrNull { it.name.startsWith("set") }?.let {
                            bindings.put(METHOD_FOR_FIR_FUNCTION, setter, Method(it.name, getAsmMethodSignatureWithCorrection(it)))
                        }
                    }
                    methods.singleOrNull { it.name.endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX) }?.let {
                        bindings.put(
                            FirJvmSerializerExtension.SYNTHETIC_METHOD_FOR_FIR_VARIABLE,
                            fir,
                            Method(it.name, getAsmMethodSignatureWithCorrection(it))
                        )
                    }
                }
                else -> {}
            }
        }
    }

    private fun createTopLevelSerializer(
        metadataSource: FirMetadataSource,
        bindings: JvmSerializationBindings,
    ): Pair<FirElementSerializer, JvmStringTable> {
        val stringTable = FirJvmElementAwareStringTableForLightClasses()
        val jvmSerializerExtension = FirJvmSerializerExtension(
            firSession,
            bindings,
            metadataSource,
            localDelegatedProperties = emptyList(),
            firSession.typeApproximator,
            scopeSession,
            JvmSerializationBindings(),
            useTypeTable = true,
            moduleName = (analysisSession.useSiteModule as KtSourceModule).run { stableModuleName ?: moduleName },
            classBuilderMode = ClassBuilderMode.KAPT3,
            isParamAssertionsDisabled = false,
            unifiedNullChecks = true,
            metadataVersion = metadataVersion,
            // Technically we should use JvmDefaultMode.ALL_INCOMPATIBLE because Kapt4 always uses `-Xjvm-default=all`, but it would make
            // the majority of tests fail because metadata of each interface will have a flag set, compared to Kapt3.
            jvmDefaultMode = JvmDefaultMode.DEFAULT,
            stringTable,
            null,
            null
        )
        return FirElementSerializer.createTopLevel(
            firSession,
            scopeSession,
            jvmSerializerExtension,
            firSession.typeApproximator,
            firSession.languageVersionSettings
        ) to stringTable
    }

    private fun generateAnnotation(message: GeneratedMessageLite, stringTable: JvmStringTable, kind: KotlinClassHeader.Kind): Metadata =
        Metadata(
            kind = kind.id,
            metadataVersion = metadataVersion.toArray(),
            data1 = JvmProtoBufUtil.writeData(message, stringTable),
            data2 = stringTable.strings.toTypedArray(),
            extraInt = FirJvmBackendExtension.generateMetadataExtraFlags(JvmAbiStability.STABLE) or
                    generateLanguageVersionSettingsBasedMetadataFlags(firSession.languageVersionSettings)
        )
}

private fun getAsmMethodSignatureWithCorrection(method: PsiMethod): String = buildString {
    append("(")
    if (method.containingClass?.isEnum == true && method.isConstructor) {
        // Enum constructors are represented without name/ordinal in light classes, which seems fine because they don't have name/ordinal
        // in Java sources as well, even though the parameters are there in the bytecode. Since metadata stores JVM signatures, we're
        // adding the name/ordinal parameters manually.
        append("Ljava/lang/String;")
        append("I")
    }
    for (param in method.parameterList.parameters) {
        append(getBinaryPresentationWithCorrection(param.type))
    }
    append(")")
    append(getBinaryPresentationWithCorrection(Optional.ofNullable(method.returnType).orElse(PsiType.VOID)))
}

private fun getBinaryPresentationWithCorrection(psiType: PsiType): String =
    ClassUtil.getBinaryPresentation(psiType).takeIf { it.isNotEmpty() } ?: "L${NON_EXISTENT_CLASS_NAME};"

private class FirJvmElementAwareStringTableForLightClasses : JvmStringTable(), FirElementAwareStringTable {
    override fun getLocalClassIdReplacement(firClass: FirClass): ClassId {
        return firClass.classId
    }
}
