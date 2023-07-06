/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.analysis.api.components.KtMetadataCalculator
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmSerializerExtension
import org.jetbrains.kotlin.fir.backend.jvm.jvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isSynthetic
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.typeApproximator
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.toMetadataVersion
import org.jetbrains.org.objectweb.asm.commons.Method

internal class KtFirMetadataCalculator(
    override val analysisSession: KtFirAnalysisSession
) : KtMetadataCalculator(), KtFirAnalysisSessionComponent {
    private val firSession: FirSession
        get() = firResolveSession.useSiteFirSession

    private val scopeSession: ScopeSession
        get() = firResolveSession.getScopeSessionFor(firSession)

    private val targetMetadataVersion: BinaryVersion
        get() = metadataVersion ?: firSession.languageVersionSettings.languageVersion.toMetadataVersion()


    override fun calculate(ktClass: KtClassOrObject): Metadata {
        // TODO: support nested classes
//        val firClasses = ktClass.parentsWithSelf
//            .filterIsInstance<KtClassOrObject>()
//            .map { it.getOrBuildFirOfType<FirRegularClass>(firResolveSession) }
//            .toList()
        val firClass = ktClass.getOrBuildFirOfType<FirRegularClass>(firResolveSession)
        val bindings = JvmSerializationBindings()
        firClass.accept(bindingsCollector(bindings))
        val (serializer, stringTable) = createTopLevelSerializer(FirMetadataSource.Class(firClass), bindings)
        val classProto = serializer.classProto(firClass)
        return generateAnnotation(classProto.build(), stringTable, Kind.Class)
    }

    override fun calculate(ktFile: KtFile): Metadata {
        return calculate(listOf(ktFile))
    }

    override fun calculate(ktFiles: Collection<KtFile>): Metadata {
        val firFiles = ktFiles.map { it.getOrBuildFirFile(firResolveSession) }
        val bindings = JvmSerializationBindings()
        val collector = bindingsCollector(bindings)
        firFiles.forEach { it.accept(collector) }
        val (serializer, stringTable) = createTopLevelSerializer(FirMetadataSource.File(firFiles), bindings)
        val fileProto = serializer.packagePartProto(firFiles.first().packageFqName, firFiles, null)

        return generateAnnotation(fileProto.build(), stringTable, Kind.File)
    }

    private enum class Kind(val value: Int) {
        Class(1),
        File(2),
        SyntheticClass(3),
        MultifileClassFacade(4),
        MultifileClassPart(5)
    }

    private fun generateAnnotation(message: GeneratedMessageLite, stringTable: JvmStringTable, kind: Kind): Metadata {
        val languageVersionSettings = firSession.languageVersionSettings
        var flags = JvmAnnotationNames.METADATA_JVM_IR_FLAG or JvmAnnotationNames.METADATA_FIR_FLAG or
                JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG // TODO: check compiler configuration
        if (languageVersionSettings.isPreRelease()) {
            flags = flags or JvmAnnotationNames.METADATA_PRE_RELEASE_FLAG
        }
        if (languageVersionSettings.getFlag(JvmAnalysisFlags.strictMetadataVersionSemantics)) {
            flags = flags or JvmAnnotationNames.METADATA_STRICT_VERSION_SEMANTICS_FLAG
        }
        return Metadata(
            kind = kind.value,
            metadataVersion = targetMetadataVersion.toArray(),
            data1 = JvmProtoBufUtil.writeData(message, stringTable),
            data2 = ArrayUtil.toStringArray(stringTable.strings),
            extraString = "",
            packageName = "",
            extraInt = flags
        )
    }

    private fun createTopLevelSerializer(metadata: FirMetadataSource, bindings: JvmSerializationBindings): Pair<FirElementSerializer, JvmStringTable> {
        val session = firSession
        val scopeSession = scopeSession
        val typeApproximator = session.typeApproximator
        val stringTable = FirJvmElementAwareStringTableForLightClasses()
        val jvmSerializerExtension = FirJvmSerializerExtension(
            session,
            bindings,
            metadata,
            localDelegatedProperties = emptyList(),
            typeApproximator,
            scopeSession,
            JvmSerializationBindings(),
            useTypeTable = true,
            moduleName = analysisSession.useSiteModule.moduleDescription,
            classBuilderMode = ClassBuilderMode.KAPT3,
            isParamAssertionsDisabled = true,
            unifiedNullChecks = false,
            metadataVersion = targetMetadataVersion,
            jvmDefaultMode = JvmDefaultMode.ENABLE,
            stringTable,
            null,
            null
        )
        return FirElementSerializer.createTopLevel(
            session,
            scopeSession,
            jvmSerializerExtension,
            typeApproximator,
            session.languageVersionSettings
        ) to stringTable
    }

    private class FirJvmElementAwareStringTableForLightClasses : JvmStringTable(), FirElementAwareStringTable {
        override fun getLocalClassIdReplacement(firClass: FirClass): ClassId {
            return firClass.classId
        }
    }

    private fun bindingsCollector(bindings: JvmSerializationBindings) = object : FirVisitorVoid() {
        private val session = firSession

        override fun visitElement(element: FirElement) {
            (element as? FirDeclaration)?.symbol?.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            element.acceptChildren(this)
        }

        override fun visitProperty(property: FirProperty) {
            super.visitProperty(property)
            property.backingField?.let {
                val name = if (property.delegate != null) "${property.name.asString()}\$delegate" else property.name.asString()
                bindings.put(
                    FirJvmSerializerExtension.FIELD_FOR_PROPERTY,
                    property,
                    session.jvmTypeMapper.mapType(it.returnTypeRef.coneType) to name
                )
            }
        }

        override fun visitFunction(function: FirFunction) {
            super.visitFunction(function)
            val name = (function as? FirSimpleFunction)?.name?.asString() ?: "<init>"
            val method = Method(name, function.computeJvmDescriptor(""))
            when {
                name.endsWith("\$annotations") ->
                    findProperty(function)?.let {
                        bindings.put(FirJvmSerializerExtension.SYNTHETIC_METHOD_FOR_FIR_VARIABLE, it, method)
                    }
                name.endsWith("\$delegate") ->
                    findProperty(function)?.let {
                        bindings.put(FirJvmSerializerExtension.SYNTHETIC_METHOD_FOR_FIR_VARIABLE, it, method)
                    }
            }
            bindings.put(FirJvmSerializerExtension.METHOD_FOR_FIR_FUNCTION, function, method)
        }

        private fun findProperty(function: FirFunction): FirProperty? {
            if (!function.isSynthetic) return null
            val functionName = (function as FirSimpleFunction).name.asString()
            val propertyName = functionName.substring(0, functionName.lastIndexOf('$'))
            return function.getContainingClass(firSession)
                ?.declarations
                ?.filterIsInstance<FirProperty>()
                ?.singleOrNull { it.name.asString() == propertyName }
        }
    }
}
