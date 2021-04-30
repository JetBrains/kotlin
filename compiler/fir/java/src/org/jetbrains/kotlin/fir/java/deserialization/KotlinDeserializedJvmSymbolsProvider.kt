/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.topLevelName
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import java.nio.file.Path
import java.nio.file.Paths

@ThreadSafeMutableState
class KotlinDeserializedJvmSymbolsProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val packagePartProvider: PackagePartProvider,
    private val kotlinClassFinder: KotlinClassFinder,
    private val javaSymbolProvider: JavaSymbolProvider,
    javaClassFinder: JavaClassFinder,
) : AbstractFirDeserializedSymbolsProvider(session, moduleDataProvider, kotlinScopeProvider) {
    private val knownNameInPackageCache = KnownNameInPackageCache(session, javaClassFinder)
    private val annotationsLoader = AnnotationsLoader(session)

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        return packagePartProvider.findPackageParts(packageFqName.asString()).mapNotNull { partName ->
            val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
            if (knownNameInPackageCache.hasNoTopLevelClassOf(classId)) return@mapNotNull null
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
                    FirConstDeserializer(session, facadeBinaryClass ?: kotlinJvmBinaryClass),
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

    override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
        if (knownNameInPackageCache.hasNoTopLevelClassOf(classId)) return null
        val result = try {
            kotlinClassFinder.findKotlinClassOrContent(classId)
        } catch (e: ProcessCanceledException) {
            return null
        }
        val kotlinClass = when (result) {
            is KotlinClassFinder.Result.KotlinClass -> result
            is KotlinClassFinder.Result.ClassFileContent -> {
                return ClassMetadataFindResult.ClassWithoutMetadata(readClassFromClassFile(classId, result))
            }
            null -> return ClassMetadataFindResult.ShouldDeserializeViaParent
        }
        if (kotlinClass.kotlinJvmBinaryClass.classHeader.kind != KotlinClassHeader.Kind.CLASS) return null
        val (nameResolver, classProto) = kotlinClass.extractMetadata() ?: return null

        if (parentContext == null && Flags.CLASS_KIND.get(classProto.flags) == ProtoBuf.Class.Kind.COMPANION_OBJECT) {
            return ClassMetadataFindResult.ShouldDeserializeViaParent
        }

        return ClassMetadataFindResult.Metadata(
            nameResolver,
            classProto,
            JvmBinaryAnnotationDeserializer(session, kotlinClass.kotlinJvmBinaryClass, kotlinClassFinder, kotlinClass.byteContent),
            kotlinClass.kotlinJvmBinaryClass.containingLibrary.toPath(),
            KotlinJvmBinarySourceElement(kotlinClass.kotlinJvmBinaryClass),
            classPostProcessor = { loadAnnotationsFromClassFile(kotlinClass, it) }
        )
    }

    private fun loadAnnotationsFromClassFile(
        kotlinClass: KotlinClassFinder.Result.KotlinClass,
        symbol: FirRegularClassSymbol
    ) {
        val annotations = mutableListOf<FirAnnotationCall>()
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
        (symbol.fir.annotations as MutableList<FirAnnotationCall>) += annotations
    }

    private fun readClassFromClassFile(classId: ClassId, classFile: KotlinClassFinder.Result.ClassFileContent): FirRegularClassSymbol? {
        return javaSymbolProvider.getFirJavaClass(classId, classFile)
    }

    private fun KotlinClassFinder.Result.KotlinClass.extractMetadata(): Pair<NameResolver, ProtoBuf.Class>? {
        val data = kotlinJvmBinaryClass.classHeader.data ?: return null
        val strings = kotlinJvmBinaryClass.classHeader.strings ?: return null
        return JvmProtoBufUtil.readClassDataFrom(data, strings)
    }

    private class KnownNameInPackageCache(
        session: FirSession,
        private val javaClassFinder: JavaClassFinder
    ) {
        private val knownClassNamesInPackage = session.firCachesFactory.createCache(javaClassFinder::knownClassNamesInPackage)

        /**
         * This function returns true if we are sure that no top-level class with this id is available
         * If it returns false, it means we can say nothing about this id
         */
        fun hasNoTopLevelClassOf(classId: ClassId): Boolean {
            val knownNames = knownClassNamesInPackage.getValue(classId.packageFqName) ?: return false
            return classId.relativeClassName.topLevelName() !in knownNames
        }
    }

    private fun String?.toPath(): Path? {
        return this?.let { Paths.get(it) }
    }
}
