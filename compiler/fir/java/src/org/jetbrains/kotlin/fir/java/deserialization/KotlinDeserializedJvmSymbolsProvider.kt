/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.deserialization.FirConstDeserializer
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.deserializeClassToSymbol
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.topLevelName
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

@ThreadSafeMutableState
class KotlinDeserializedJvmSymbolsProvider(
    session: FirSession,
    val project: Project,
    private val packagePartProvider: PackagePartProvider,
    private val javaSymbolProvider: JavaSymbolProvider,
    private val kotlinClassFinder: KotlinClassFinder,
    private val javaClassFinder: JavaClassFinder,
    private val kotlinScopeProvider: KotlinScopeProvider,
) : FirSymbolProvider(session) {
    private val annotationsLoader = AnnotationsLoader(session)
    private val classCache = SymbolProviderCache<ClassId, FirRegularClassSymbol>()
    private val typeAliasCache = SymbolProviderCache<ClassId, FirTypeAliasSymbol>()
    private val packagePartsCache = SymbolProviderCache<FqName, Collection<PackagePartsCacheData>>()

    private val knownNameInPackageCache = KnownNameInPackageCache(session, javaClassFinder)

    // TODO: implement thread safety for this property
    private val handledByJava = HashSet<ClassId>()

    private class PackagePartsCacheData(
        val proto: ProtoBuf.Package,
        val context: FirDeserializationContext,
    ) {
        val topLevelFunctionNameIndex by lazy {
            proto.functionList.withIndex()
                .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
        }
        val topLevelPropertyNameIndex by lazy {
            proto.propertyList.withIndex()
                .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
        }
        val typeAliasNameIndex by lazy {
            proto.typeAliasList.withIndex()
                .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
        }
    }


    private fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {

        return packagePartProvider.findPackageParts(packageFqName.asString()).mapNotNull { partName ->
            val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
            if (knownNameInPackageCache.hasNoTopLevelClassOf(classId)) return@mapNotNull null
            val (kotlinJvmBinaryClass, byteContent) =
                kotlinClassFinder.findKotlinClassOrContent(classId) as? KotlinClassFinder.Result.KotlinClass ?: return@mapNotNull null

            val facadeName = kotlinJvmBinaryClass.classHeader.multifileClassName?.takeIf { it.isNotEmpty() }
            val facadeFqName = facadeName?.let { JvmClassName.byInternalName(it).fqNameForTopLevelClassMaybeWithDollars }
            val facadeBinaryClass = facadeFqName?.let { kotlinClassFinder.findKotlinClass(ClassId.topLevel(it)) }

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
                    packageFqName, packageProto, nameResolver, session,
                    JvmBinaryAnnotationDeserializer(session, kotlinJvmBinaryClass, byteContent),
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

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return findAndDeserializeClass(classId) ?: findAndDeserializeTypeAlias(classId)
    }

    private fun findAndDeserializeTypeAlias(
        classId: ClassId,
    ): FirTypeAliasSymbol? {
        if (!classId.relativeClassName.isOneSegmentFQN()) return null
        return typeAliasCache.lookupCacheOrCalculate(classId) {
            getPackageParts(classId.packageFqName).firstNotNullResult { part ->
                val ids = part.typeAliasNameIndex[classId.shortClassName]
                if (ids == null || ids.isEmpty()) return@firstNotNullResult null
                val aliasProto = ids.map { part.proto.getTypeAlias(it) }.single()
                part.context.memberDeserializer.loadTypeAlias(aliasProto).symbol
            }
        }
    }

    private fun KotlinJvmBinaryClass.readClassDataFrom(): Pair<JvmNameResolver, ProtoBuf.Class>? {
        val data = classHeader.data ?: return null
        val strings = classHeader.strings ?: return null
        return JvmProtoBufUtil.readClassDataFrom(data, strings)
    }


    private fun findAndDeserializeClassViaParent(classId: ClassId): FirRegularClassSymbol? {
        val outerClassId = classId.outerClassId ?: return null
        findAndDeserializeClass(outerClassId) ?: return null
        return classCache[classId]
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): FirRegularClassSymbol? {
        if (knownNameInPackageCache.hasNoTopLevelClassOf(classId)) return null
        if (classId in classCache) return classCache[classId]

        if (classId in handledByJava) return null

        val result = try {
            kotlinClassFinder.findKotlinClassOrContent(classId)
        } catch (e: ProcessCanceledException) {
            return null
        }
        val (kotlinJvmBinaryClass, byteContent) = when (result) {
            is KotlinClassFinder.Result.KotlinClass -> result
            is KotlinClassFinder.Result.ClassFileContent -> {
                handledByJava.add(classId)
                return try {
                    javaSymbolProvider.getFirJavaClass(classId, result)
                } catch (e: ProcessCanceledException) {
                    null
                }
            }
            null -> return findAndDeserializeClassViaParent(classId)
        }
        if (kotlinJvmBinaryClass.classHeader.kind != KotlinClassHeader.Kind.CLASS) return null
        val (nameResolver, classProto) = kotlinJvmBinaryClass.readClassDataFrom() ?: return null

        if (parentContext == null && Flags.CLASS_KIND.get(classProto.flags) == ProtoBuf.Class.Kind.COMPANION_OBJECT) {
            return findAndDeserializeClassViaParent(classId)
        }

        val symbol = FirRegularClassSymbol(classId)
        deserializeClassToSymbol(
            classId, classProto, symbol, nameResolver, session,
            JvmBinaryAnnotationDeserializer(session, kotlinJvmBinaryClass, byteContent),
            kotlinScopeProvider,
            parentContext, KotlinJvmBinarySourceElement(kotlinJvmBinaryClass),
            this::findAndDeserializeClass
        )

        classCache[classId] = symbol
        val annotations = mutableListOf<FirAnnotationCall>()
        kotlinJvmBinaryClass.loadClassAnnotations(
            object : KotlinJvmBinaryClass.AnnotationVisitor {
                override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return annotationsLoader.loadAnnotationIfNotSpecial(classId, annotations)
                }

                override fun visitEnd() {
                }
            },
            byteContent,
        )
        (symbol.fir.annotations as MutableList<FirAnnotationCall>) += annotations
        return symbol
    }

    private fun loadFunctionsByName(part: PackagePartsCacheData, name: Name): List<FirNamedFunctionSymbol> {
        val functionIds = part.topLevelFunctionNameIndex[name] ?: return emptyList()
        return functionIds.map {
            part.context.memberDeserializer.loadFunction(part.proto.getFunction(it)).symbol
        }
    }

    private fun loadPropertiesByName(part: PackagePartsCacheData, name: Name): List<FirPropertySymbol> {
        val propertyIds = part.topLevelPropertyNameIndex[name] ?: return emptyList()
        return propertyIds.map {
            part.context.memberDeserializer.loadProperty(part.proto.getProperty(it)).symbol
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        getPackageParts(packageFqName).flatMapTo(destination) { part ->
            loadFunctionsByName(part, name) + loadPropertiesByName(part, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getPackageParts(packageFqName).flatMapTo(destination) { part ->
            loadFunctionsByName(part, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        getPackageParts(packageFqName).flatMapTo(destination) { part ->
            loadPropertiesByName(part, name)
        }
    }

    private fun getPackageParts(packageFqName: FqName): Collection<PackagePartsCacheData> {
        return packagePartsCache.lookupCacheOrCalculate(packageFqName) {
            try {
                computePackagePartsInfos(packageFqName)
            } catch (e: ProcessCanceledException) {
                emptyList()
            }
        }!!
    }

    override fun getPackage(fqName: FqName): FqName? = null
}

private class KnownNameInPackageCache(session: FirSession, private val javaClassFinder: JavaClassFinder) {
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