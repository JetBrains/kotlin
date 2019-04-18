/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.deserializeClassToSymbol
import org.jetbrains.kotlin.fir.java.topLevelName
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.findKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class KotlinDeserializedJvmSymbolsProvider(
    val session: FirSession,
    val project: Project,
    private val packagePartProvider: PackagePartProvider,
    private val kotlinClassFinder: KotlinClassFinder,
    private val javaClassFinder: JavaClassFinder
) : AbstractFirSymbolProvider() {

    private val classesCache = mutableMapOf<ClassId, FirClassSymbol>()
    private val typeAliasCache = mutableMapOf<ClassId, FirTypeAliasSymbol?>()
    private val packagePartsCache = mutableMapOf<FqName, Collection<PackagePartsCacheData>>()

    private class PackagePartsCacheData(val proto: ProtoBuf.Package, val context: FirDeserializationContext) {
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

    private val knownClassNamesInPackage = mutableMapOf<FqName, Set<String>?>()

    private fun hasTopLevelClassOf(classId: ClassId): Boolean {
        val knownNames = knownClassNamesInPackage.getOrPut(classId.packageFqName) {
            javaClassFinder.knownClassNamesInPackage(classId.packageFqName)
        } ?: return false
        return classId.relativeClassName.topLevelName() in knownNames
    }

    private fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {

        return packagePartProvider.findPackageParts(packageFqName.asString()).mapNotNull { partName ->
            val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
            if (!hasTopLevelClassOf(classId)) return@mapNotNull null
            val kotlinJvmBinaryClass = kotlinClassFinder.findKotlinClass(classId) ?: return@mapNotNull null

            val data = kotlinJvmBinaryClass.classHeader.data ?: return@mapNotNull null
            val strings = kotlinJvmBinaryClass.classHeader.strings ?: return@mapNotNull null
            val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(data, strings)

            PackagePartsCacheData(
                packageProto,
                FirDeserializationContext.createForPackage(packageFqName, packageProto, nameResolver, session)
            )
        }
    }

    override fun getClassUseSiteMemberScope(
        classId: ClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        val symbol = this.getClassLikeSymbolByFqName(classId) ?: return null

        return symbol.firUnsafe<FirRegularClass>().buildDefaultUseSiteScope(session, scopeSession)
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return findAndDeserializeClass(classId) ?: findAndDeserializeTypeAlias(classId)
    }

    private fun findAndDeserializeTypeAlias(
        classId: ClassId
    ): FirTypeAliasSymbol? {
        return typeAliasCache.getOrPut(classId) {
            getPackageParts(classId.packageFqName).firstNotNullResult { part ->
                val ids = part.typeAliasNameIndex[classId.shortClassName]
                if (ids == null || ids.isEmpty()) return@firstNotNullResult null
                val aliasProto = ids.map { part.proto.getTypeAlias(it) }.single()

                part.context.memberDeserializer.loadTypeAlias(aliasProto).symbol
            }
        }
    }


    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): FirClassSymbol? {
        if (!hasTopLevelClassOf(classId)) return null
        return classesCache.getOrPut(classId) {
            //return null
            val kotlinJvmBinaryClass = kotlinClassFinder.findKotlinClass(classId) ?: return null
            if (kotlinJvmBinaryClass.classHeader.kind != KotlinClassHeader.Kind.CLASS) return null

            val data = kotlinJvmBinaryClass.classHeader.data ?: return null
            val strings = kotlinJvmBinaryClass.classHeader.strings ?: return null
            val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(data, strings)

            val symbol = FirClassSymbol(classId)
            deserializeClassToSymbol(
                classId, classProto, symbol, nameResolver, session, parentContext,
                this::findAndDeserializeClass
            )
            symbol
        }
    }

    private fun loadFunctionsByName(part: PackagePartsCacheData, name: Name): List<FirCallableSymbol> {
        val functionIds = part.topLevelFunctionNameIndex[name] ?: return emptyList()
        return functionIds.map { part.proto.getFunction(it) }
            .map {
                part.context.memberDeserializer.loadFunction(it).symbol
            }
    }

    private fun loadPropertiesByName(part: PackagePartsCacheData, name: Name): List<FirCallableSymbol> {
        val propertyIds = part.topLevelPropertyNameIndex[name] ?: return emptyList()
        return propertyIds.map { part.proto.getProperty(it) }
            .map {
                part.context.memberDeserializer.loadProperty(it).symbol
            }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<ConeCallableSymbol> {
        return getPackageParts(packageFqName).flatMap { part ->
            loadFunctionsByName(part, name) + loadPropertiesByName(part, name)
        }
    }

    override fun getClassDeclaredMemberScope(classId: ClassId) =
        findRegularClass(classId)?.let(::FirClassDeclaredMemberScope)

    private fun getPackageParts(packageFqName: FqName): Collection<PackagePartsCacheData> {
        return packagePartsCache.getOrPut(packageFqName) {
            computePackagePartsInfos(packageFqName)
        }
    }

    override fun getAllCallableNamesInPackage(fqName: FqName): Set<Name> {
        return getPackageParts(fqName).flatMapTo(mutableSetOf()) { packagePart ->
            packagePart.proto.functionList.map { packagePart.context.nameResolver.getName(it.name) }
        }
    }

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        javaClassFinder.findPackage(fqName)
            ?.getClasses { true }.orEmpty()
            .mapTo(sortedSetOf(), JavaClass::name)

    private fun getClassDeclarations(classId: ClassId): List<FirDeclaration> {
        return findRegularClass(classId)?.declarations ?: emptyList()
    }


    private fun findRegularClass(classId: ClassId): FirRegularClass? =
        @Suppress("UNCHECKED_CAST")
        (getClassLikeSymbolByFqName(classId) as? FirBasedSymbol<FirRegularClass>)?.fir

    override fun getAllCallableNamesInClass(classId: ClassId): Set<Name> =
        getClassDeclarations(classId)
            .filterIsInstance<FirNamedDeclaration>()
            .mapTo(mutableSetOf(), FirNamedDeclaration::name)

    override fun getNestedClassesNamesInClass(classId: ClassId): Set<Name> {
        return getClassDeclarations(classId).filterIsInstance<FirRegularClass>().mapTo(mutableSetOf()) { it.name }
    }

    override fun getPackage(fqName: FqName): FqName? = null
}
