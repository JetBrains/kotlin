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
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getOrPut
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
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

class KotlinDeserializedJvmSymbolsProvider(
    val session: FirSession,
    val project: Project,
    private val packagePartProvider: PackagePartProvider,
    private val kotlinClassFinder: KotlinClassFinder,
    private val javaClassFinder: JavaClassFinder
) : AbstractFirSymbolProvider() {

    private val classesCache = mutableMapOf<ClassId, FirClassSymbol>()
    private val packagePartsCache = mutableMapOf<FqName, Collection<Pair<ProtoBuf.Package, FirDeserializationContext>>>()

    private fun computePackagePartsInfos(packageFqName: FqName): List<Pair<ProtoBuf.Package, FirDeserializationContext>> {
        return packagePartProvider.findPackageParts(packageFqName.asString()).mapNotNull { partName ->
            val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
            val kotlinJvmBinaryClass = kotlinClassFinder.findKotlinClass(classId) ?: return@mapNotNull null

            val data = kotlinJvmBinaryClass.classHeader.data ?: return@mapNotNull null
            val strings = kotlinJvmBinaryClass.classHeader.strings ?: return@mapNotNull null
            val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(data, strings)

            packageProto to FirDeserializationContext.createForPackage(packageFqName, packageProto, nameResolver, session)
        }
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return findAndDeserializeClass(classId)
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): FirClassSymbol? {
        val kotlinJvmBinaryClass = kotlinClassFinder.findKotlinClass(classId) ?: return null
        if (kotlinJvmBinaryClass.classHeader.kind != KotlinClassHeader.Kind.CLASS) return null

        val data = kotlinJvmBinaryClass.classHeader.data ?: return null
        val strings = kotlinJvmBinaryClass.classHeader.strings ?: return null
        val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(data, strings)

        return classesCache.getOrPut(classId, { FirClassSymbol(classId) }) { symbol ->
            deserializeClassToSymbol(
                classId, classProto, symbol, nameResolver, session, parentContext,
                this::findAndDeserializeClass
            )
        }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<ConeCallableSymbol> {
        return getPackageParts(packageFqName).flatMap { (packageProto, context) ->
            packageProto.functionList.map {
                context.memberDeserializer.loadFunction(it).symbol
            }.filter { callableSymbol -> callableSymbol.callableId.callableName == name }
        }
    }

    override fun getClassDeclaredMemberScope(classId: ClassId) =
        findRegularClass(classId)?.let(::FirClassDeclaredMemberScope)

    private fun getPackageParts(packageFqName: FqName): Collection<Pair<ProtoBuf.Package, FirDeserializationContext>> {
        return packagePartsCache.getOrPut(packageFqName) {
            computePackagePartsInfos(packageFqName)
        }
    }

    override fun getAllCallableNamesInPackage(fqName: FqName): Set<Name> {
        return getPackageParts(fqName).flatMapTo(mutableSetOf()) { (packageProto, context) ->
            packageProto.functionList.map { context.nameResolver.getName(it.name) }
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
