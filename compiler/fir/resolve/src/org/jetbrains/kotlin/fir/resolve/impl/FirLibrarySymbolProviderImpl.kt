/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.deserializeClassToSymbol
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getOrPut
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.io.InputStream

class FirLibrarySymbolProviderImpl(val session: FirSession) : FirSymbolProvider {
    private class BuiltInsPackageFragment(stream: InputStream, val fqName: FqName, val session: FirSession) {
        lateinit var version: BuiltInsBinaryVersion

        val packageProto: ProtoBuf.PackageFragment = run {

            version = BuiltInsBinaryVersion.readFrom(stream)

            if (!version.isCompatible()) {
                // TODO: report a proper diagnostic
                throw UnsupportedOperationException(
                    "Kotlin built-in definition format version is not supported: " +
                            "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                            "Please update Kotlin"
                )
            }

            ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
        }

        private val nameResolver = NameResolverImpl(packageProto.strings, packageProto.qualifiedNames)

        val classDataFinder = ProtoBasedClassDataFinder(packageProto, nameResolver, version) { SourceElement.NO_SOURCE }

        private val memberDeserializer by lazy {
            FirDeserializationContext.createForPackage(fqName, packageProto.`package`, nameResolver, session).memberDeserializer
        }

        val lookup = mutableMapOf<ClassId, FirClassSymbol>()

        fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? =
            findAndDeserializeClass(classId)

        private fun findAndDeserializeClass(
            classId: ClassId,
            parentContext: FirDeserializationContext? = null
        ): FirClassSymbol? {
            if (classId !in classDataFinder.allClassIds) return null
            return lookup.getOrPut(classId, { FirClassSymbol(classId) }) { symbol ->
                val classData = classDataFinder.findClassData(classId)!!
                val classProto = classData.classProto

                deserializeClassToSymbol(
                    classId, classProto, symbol, nameResolver, session,
                    parentContext,
                    this::findAndDeserializeClass
                )
            }
        }

        fun getTopLevelCallableSymbols(name: Name): List<ConeCallableSymbol> {
            return packageProto.`package`.functionList.filter { nameResolver.getName(it.name) == name }.map {
                memberDeserializer.loadFunction(it).symbol
            }
        }

        fun getAllCallableNames(): Set<Name> {
            return packageProto.`package`.functionList.mapTo(mutableSetOf()) { nameResolver.getName(it.name) }
        }

        fun getAllClassNames(): Set<Name> {
            return classDataFinder.allClassIds.mapTo(mutableSetOf()) { it.shortClassName }
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        if (allPackageFragments.containsKey(fqName)) return fqName
        return null
    }

    private fun loadBuiltIns(): List<BuiltInsPackageFragment> {
        val classLoader = this::class.java.classLoader
        val streamProvider = { path: String -> classLoader?.getResourceAsStream(path) ?: ClassLoader.getSystemResourceAsStream(path) }
        val packageFqNames = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAMES

        return packageFqNames.map { fqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(fqName)
            val inputStream = streamProvider(resourcePath) ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
            BuiltInsPackageFragment(inputStream, fqName, session)
        }
    }

    private val allPackageFragments = loadBuiltIns().groupBy { it.fqName }

    private val fictitiousFunctionSymbols = mutableMapOf<Int, ConeClassSymbol>()

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return allPackageFragments[classId.packageFqName]?.firstNotNullResult {
            it.getClassLikeSymbolByFqName(classId)
        } ?: with(classId) {
            val className = relativeClassName.asString()
            val kind = FunctionClassDescriptor.Kind.byClassNamePrefix(packageFqName, className) ?: return@with null
            val prefix = kind.classNamePrefix
            val arity = className.substring(prefix.length).toIntOrNull() ?: return null
            fictitiousFunctionSymbols.getOrPut(arity) {
                FirClassSymbol(this).apply {
                    FirClassImpl(
                        session,
                        null,
                        this,
                        relativeClassName.shortName(),
                        Visibilities.PUBLIC,
                        Modality.OPEN,
                        false,
                        false,
                        ClassKind.CLASS,
                        isInner = false,
                        isCompanion = false,
                        isData = false,
                        isInline = false
                    )
                }
            }
        }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<ConeCallableSymbol> {
        return allPackageFragments[packageFqName]?.flatMap {
            it.getTopLevelCallableSymbols(name)
        } ?: emptyList()
    }

    override fun getClassDeclaredMemberScope(classId: ClassId): FirScope? =
        findRegularClass(classId)?.let(::FirClassDeclaredMemberScope)

    override fun getAllCallableNamesInPackage(fqName: FqName): Set<Name> {
        return allPackageFragments[fqName]?.flatMapTo(mutableSetOf()) {
            it.getAllCallableNames()
        } ?: emptySet()
    }

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> {
        return allPackageFragments[fqName]?.flatMapTo(mutableSetOf()) {
            it.getAllClassNames()
        } ?: emptySet()
    }

    override fun getAllCallableNamesInClass(classId: ClassId): Set<Name> {
        return getClassDeclarations(classId).filterIsInstance<FirCallableMemberDeclaration>().mapTo(mutableSetOf()) { it.name }
    }

    private fun getClassDeclarations(classId: ClassId): List<FirDeclaration> {
        return findRegularClass(classId)?.declarations ?: emptyList()
    }


    private fun findRegularClass(classId: ClassId): FirRegularClass? =
        @Suppress("UNCHECKED_CAST")
        (getClassLikeSymbolByFqName(classId) as? FirBasedSymbol<FirRegularClass>)?.fir

    override fun getNestedClassesNamesInClass(classId: ClassId): Set<Name> {
        return getClassDeclarations(classId).filterIsInstance<FirRegularClass>().mapTo(mutableSetOf()) { it.name }
    }
}
