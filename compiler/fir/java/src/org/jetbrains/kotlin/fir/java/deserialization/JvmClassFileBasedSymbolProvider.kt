/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.getDeprecationInfos
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Any top level declarations in core/builtins/src are also available from FirBuiltinSymbolProvider (or FirIdeBuiltinSymbolProvider) for IDE
 * so we filter them out to avoid providing the "same" symbols twice.
 */
private val kotlinBuiltins = setOf("kotlin/ArrayIntrinsicsKt", "kotlin/internal/ProgressionUtilKt")

// This symbol provider loads JVM classes, reading extra info from Kotlin `@Metadata` annotations
// if present. Use it for library and incremental compilation sessions. For source sessions use
// `JavaSymbolProvider`, as Kotlin classes should be parsed first.
@ThreadSafeMutableState
class JvmClassFileBasedSymbolProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val packagePartProvider: PackagePartProvider,
    private val kotlinClassFinder: KotlinClassFinder,
    private val javaFacade: FirJavaFacade,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
) : AbstractFirDeserializedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin) {
    private val annotationsLoader = AnnotationsLoader(session, kotlinClassFinder)

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        return packagePartProvider.findPackageParts(packageFqName.asString()).mapNotNull { partName ->
            if (partName in kotlinBuiltins) return@mapNotNull null
            val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
            if (!javaFacade.hasTopLevelClassOf(classId)) return@mapNotNull null
            val (kotlinJvmBinaryClass, byteContent) =
                kotlinClassFinder.findKotlinClassOrContent(classId) as? KotlinClassFinder.Result.KotlinClass ?: return@mapNotNull null

            val facadeName = kotlinJvmBinaryClass.classHeader.multifileClassName?.takeIf { it.isNotEmpty() }
            val facadeFqName = facadeName?.let { JvmClassName.byInternalName(it).fqNameForTopLevelClassMaybeWithDollars }
            val facadeBinaryClass = facadeFqName?.let { kotlinClassFinder.findKotlinClass(ClassId.topLevel(it)) }

            val moduleData = moduleDataProvider.getModuleData(kotlinJvmBinaryClass.containingLibrary.toPath()) ?: return@mapNotNull null

            val header = kotlinJvmBinaryClass.classHeader
            val data = header.data ?: header.incompatibleData ?: return@mapNotNull null
            val strings = header.strings ?: return@mapNotNull null
            val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(data, strings)

            val source = JvmPackagePartSource(
                kotlinJvmBinaryClass, packageProto, nameResolver,
                kotlinJvmBinaryClass.incompatibility, kotlinJvmBinaryClass.isPreReleaseInvisible,
            )

            PackagePartsCacheData(
                packageProto,
                FirDeserializationContext.createForPackage(
                    packageFqName, packageProto, nameResolver, moduleData,
                    JvmBinaryAnnotationDeserializer(session, kotlinJvmBinaryClass, kotlinClassFinder, byteContent),
                    FirJvmConstDeserializer(session, facadeBinaryClass ?: kotlinJvmBinaryClass),
                    source
                ),
            )
        }
    }

    private val KotlinJvmBinaryClass.incompatibility: IncompatibleVersionErrorData<JvmMetadataVersion>?
        get() {
            // TODO: skipMetadataVersionCheck
            if (classHeader.metadataVersion.isCompatible()) return null
            return IncompatibleVersionErrorData(classHeader.metadataVersion, JvmMetadataVersion.INSTANCE, location, classId)
        }

    private val KotlinJvmBinaryClass.isPreReleaseInvisible: Boolean
        get() = classHeader.isPreRelease

    private fun loadJavaClass(classId: ClassId, content: ByteArray?): ClassMetadataFindResult? {
        val javaClass = try {
            javaFacade.findClass(classId, content) ?: return null
        } catch (e: ProcessCanceledException) {
            return null
        }
        return ClassMetadataFindResult.NoMetadata { symbol ->
            javaFacade.convertJavaClassToFir(symbol, classId.outerClassId?.let(::getClass), javaClass)
        }
    }

    override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
        // Kotlin classes are annotated Java classes, so this check also looks for them.
        if (!javaFacade.hasTopLevelClassOf(classId)) return null

        val result = try {
            kotlinClassFinder.findKotlinClassOrContent(classId)
        } catch (e: ProcessCanceledException) {
            return null
        }
        val kotlinClass = when (result) {
            is KotlinClassFinder.Result.KotlinClass -> result.kotlinJvmBinaryClass
            is KotlinClassFinder.Result.ClassFileContent -> return loadJavaClass(classId, result.content)
            null -> return loadJavaClass(classId, null)
        }
        if (kotlinClass.classHeader.kind != KotlinClassHeader.Kind.CLASS) return null
        val data = kotlinClass.classHeader.data ?: return null
        val strings = kotlinClass.classHeader.strings ?: return null
        val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(data, strings)

        return ClassMetadataFindResult.Metadata(
            nameResolver,
            classProto,
            JvmBinaryAnnotationDeserializer(session, kotlinClass, kotlinClassFinder, result.byteContent),
            kotlinClass.containingLibrary.toPath(),
            KotlinJvmBinarySourceElement(kotlinClass),
            classPostProcessor = { loadAnnotationsFromClassFile(result, it) }
        )
    }

    override fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class): Boolean =
        JvmFlags.IS_COMPILED_IN_JVM_DEFAULT_MODE.get(classProto.getExtension(JvmProtoBuf.jvmClassFlags))

    override fun getPackage(fqName: FqName): FqName? =
        javaFacade.getPackage(fqName)

    private fun loadAnnotationsFromClassFile(
        kotlinClass: KotlinClassFinder.Result.KotlinClass,
        symbol: FirRegularClassSymbol
    ) {
        val annotations = symbol.fir.annotations as MutableList<FirAnnotation>
        kotlinClass.kotlinJvmBinaryClass.loadClassAnnotations(
            object : KotlinJvmBinaryClass.AnnotationVisitor {
                override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return annotationsLoader.loadAnnotationIfNotSpecial(classId, annotations)
                }

                override fun visitEnd() {
                }
            },
            kotlinClass.byteContent,
        )
        symbol.fir.replaceDeprecation(symbol.fir.getDeprecationInfos(session.languageVersionSettings.apiVersion))
    }

    private fun String?.toPath(): Path? {
        return this?.let { Paths.get(it).normalize() }
    }
}
