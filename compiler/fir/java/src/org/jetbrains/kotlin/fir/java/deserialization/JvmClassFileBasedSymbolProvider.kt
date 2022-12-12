/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.getDeprecationsProvider
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
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
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
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
) : AbstractFirDeserializedSymbolProvider(
    session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin, BuiltInSerializerProtocol
) {
    private val annotationsLoader = AnnotationsLoader(session, kotlinClassFinder)

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        return packagePartProvider.findPackageParts(packageFqName.asString()).mapNotNull { partName ->
            if (partName in kotlinBuiltins) return@mapNotNull null
            val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
            if (!javaFacade.hasTopLevelClassOf(classId)) return@mapNotNull null

            val (kotlinJvmBinaryClass, byteContentRef) =
                kotlinClassFinder.findKotlinClassOrContent(classId) as? KotlinClassFinder.Result.KotlinClass ?: return@mapNotNull null
            // We will not use byte contents, since it is not needed anymore for most of the package part load requests
            byteContentRef?.release()

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
                    // In most cases package parts annotations are never getting loaded, so we don't eagerly read them,
                    // but will load them on demand from the disk only if needed.
                    JvmBinaryAnnotationDeserializer(session, kotlinJvmBinaryClass, kotlinClassFinder, kotlinJvmBinaryClass),
                    FirJvmConstDeserializer(session, facadeBinaryClass ?: kotlinJvmBinaryClass, BuiltInSerializerProtocol),
                    source
                ),
            )
        }
    }

    override fun computePackageSet(): Set<String> = packagePartProvider.allPackageNames().toSet()

    override fun mayHaveTopLevelClass(classId: ClassId): Boolean = javaFacade.hasTopLevelClassOf(classId)

    override fun knownTopLevelClassifiers(fqName: FqName): Set<String> {
        if (fqName.asString() !in packageNames) return javaFacade.knownTopLevelClassifiers(fqName)
        return javaFacade.knownTopLevelClassifiers(fqName) + typeAliasesNamesByPackage.getValue(fqName).map { it.asString() }
    }

    private val KotlinJvmBinaryClass.incompatibility: IncompatibleVersionErrorData<JvmMetadataVersion>?
        get() {
            // TODO: skipMetadataVersionCheck
            if (classHeader.metadataVersion.isCompatible()) return null
            return IncompatibleVersionErrorData(classHeader.metadataVersion, JvmMetadataVersion.INSTANCE, location, classId)
        }

    private val KotlinJvmBinaryClass.isPreReleaseInvisible: Boolean
        get() = classHeader.isPreRelease

    override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
        // Kotlin classes are annotated Java classes, so this check also looks for them.
        if (!javaFacade.hasTopLevelClassOf(classId)) return null

        val result = kotlinClassFinder.findKotlinClassOrContent(classId)
        try {
            if (result !is KotlinClassFinder.Result.KotlinClass) {
                if (parentContext != null || (classId.isNestedClass && getClass(classId.outermostClassId)?.fir !is FirJavaClass)) {
                    // Nested class of Kotlin class should have been a Kotlin class.
                    return null
                }
                val knownContent = (result as? KotlinClassFinder.Result.ClassFileContent)?.contentRef
                val javaClass = javaFacade.findClass(classId, knownContent) ?: return null
                return ClassMetadataFindResult.NoMetadata { symbol ->
                    javaFacade.convertJavaClassToFir(symbol, classId.outerClassId?.let(::getClass), javaClass)
                }
            }

            val kotlinClass = result.kotlinJvmBinaryClass
            if (kotlinClass.classHeader.kind != KotlinClassHeader.Kind.CLASS || kotlinClass.classId != classId) return null
            val data = kotlinClass.classHeader.data ?: return null
            val strings = kotlinClass.classHeader.strings ?: return null
            val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(data, strings)

            // In most cases, the results of extractClassMetadata get its member annotations used, so we
            // eagerly load oll of the annotations into the intermediate data structure, so that the
            // underlying byte array contents can be released
            val (memberAnnotations, classAnnotations) =
                readMemberAndClassAnnotations("classMetadata", kotlinClass, result.contentRef)

            return ClassMetadataFindResult.Metadata(
                nameResolver,
                classProto,
                JvmBinaryAnnotationDeserializer(session, kotlinClass, kotlinClassFinder, JvmMemberAnnotations(memberAnnotations)),
                moduleDataProvider.getModuleData(kotlinClass.containingLibrary?.toPath()),
                KotlinJvmBinarySourceElement(kotlinClass),
                classPostProcessor = {
                    loadAnnotationsFromClassFile(classAnnotations, it)
                }
            )
        } finally {
            result?.contentRef?.release()
        }
    }

    override fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class): Boolean =
        JvmFlags.IS_COMPILED_IN_JVM_DEFAULT_MODE.get(classProto.getExtension(JvmProtoBuf.jvmClassFlags))

    override fun getPackage(fqName: FqName): FqName? =
        javaFacade.getPackage(fqName)

    private fun loadAnnotationsFromClassFile(
        classAnnotations: List<JvmAnnotationNode>,
        symbol: FirRegularClassSymbol
    ) {
        val annotations = symbol.fir.annotations as MutableList<FirAnnotation>
        for (a in classAnnotations) {
            a.accept(annotationsLoader.loadAnnotationIfNotSpecial(a.classId, annotations))
        }
        symbol.fir.replaceDeprecationsProvider(symbol.fir.getDeprecationsProvider(session.firCachesFactory))
    }

    private fun String?.toPath(): Path? {
        return this?.let { Paths.get(it).normalize() }
    }
}
