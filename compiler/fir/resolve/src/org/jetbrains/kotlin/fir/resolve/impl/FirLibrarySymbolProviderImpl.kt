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
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.deserialization.FirBuiltinAnnotationDeserializer
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.deserializeClassToSymbol
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.buildDefaultUseSiteScope
import org.jetbrains.kotlin.fir.resolve.getOrPut
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.io.InputStream

class FirLibrarySymbolProviderImpl(val session: FirSession) : FirSymbolProvider() {
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
            FirDeserializationContext.createForPackage(
                fqName, packageProto.`package`, nameResolver, session,
                FirBuiltinAnnotationDeserializer(session)
            ).memberDeserializer
        }

        val lookup = mutableMapOf<ClassId, FirClassSymbol>()

        fun getClassLikeSymbolByFqName(classId: ClassId): FirClassSymbol? =
            findAndDeserializeClass(classId)

        private fun findAndDeserializeClass(
            classId: ClassId,
            parentContext: FirDeserializationContext? = null
        ): FirClassSymbol? {
            val classIdExists = classId in classDataFinder.allClassIds
            val shouldBeEnumEntry = !classIdExists && classId.outerClassId in classDataFinder.allClassIds
            if (!classIdExists && !shouldBeEnumEntry) return null
            if (shouldBeEnumEntry) {
                val outerClassData = classDataFinder.findClassData(classId.outerClassId!!)!!
                val outerClassProto = outerClassData.classProto
                if (outerClassProto.enumEntryList.none { nameResolver.getName(it.name) == classId.shortClassName }) {
                    return null
                }
            }
            return lookup.getOrPut(classId, { FirClassSymbol(classId) }) { symbol ->
                if (shouldBeEnumEntry) {
                    FirEnumEntryImpl(session, null, symbol, classId.shortClassName).apply {
                        resolvePhase = FirResolvePhase.DECLARATIONS
                    }
                } else {
                    val classData = classDataFinder.findClassData(classId)!!
                    val classProto = classData.classProto

                    deserializeClassToSymbol(
                        classId, classProto, symbol, nameResolver, session,
                        null, parentContext,
                        this::findAndDeserializeClass
                    )
                }
            }
        }

        fun getTopLevelCallableSymbols(name: Name): List<FirCallableSymbol<*>> {
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

    override fun getClassUseSiteMemberScope(
        classId: ClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        val symbol = this.getClassLikeSymbolByFqName(classId) ?: return null
        return symbol.fir.buildDefaultUseSiteScope(useSiteSession, scopeSession)
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

    private val fictitiousFunctionSymbols = mutableMapOf<Int, FirClassSymbol>()

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassSymbol? {
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
                        Modality.ABSTRACT,
                        isExpect = false,
                        isActual = false,
                        classKind = ClassKind.INTERFACE,
                        isInner = false,
                        isCompanion = false,
                        isData = false,
                        isInline = false
                    ).apply klass@{
                        resolvePhase = FirResolvePhase.DECLARATIONS
                        typeParameters.addAll((1..arity).map {
                            FirTypeParameterImpl(
                                this@FirLibrarySymbolProviderImpl.session,
                                null,
                                FirTypeParameterSymbol(),
                                Name.identifier("P$it"),
                                Variance.IN_VARIANCE,
                                false
                            )
                        })
                        typeParameters.add(
                            FirTypeParameterImpl(
                                this@FirLibrarySymbolProviderImpl.session,
                                null,
                                FirTypeParameterSymbol(),
                                Name.identifier("R"),
                                Variance.OUT_VARIANCE,
                                false
                            )
                        )
                        val name = OperatorNameConventions.INVOKE
                        addDeclaration(
                            FirMemberFunctionImpl(
                                this@FirLibrarySymbolProviderImpl.session,
                                null,
                                FirNamedFunctionSymbol(CallableId(packageFqName, relativeClassName, name)),
                                name,
                                Visibilities.PUBLIC,
                                Modality.ABSTRACT,
                                isExpect = false,
                                isActual = false,
                                isOverride = false,
                                isOperator = true,
                                isInfix = false,
                                isInline = false,
                                isTailRec = false,
                                isExternal = false,
                                isSuspend = false,
                                receiverTypeRef = null,
                                returnTypeRef = FirResolvedTypeRefImpl(
                                    null,
                                    ConeTypeParameterTypeImpl(
                                        typeParameters.last().symbol.toLookupTag(),
                                        false
                                    )
                                )
                            ).apply {
                                resolvePhase = FirResolvePhase.DECLARATIONS
                                valueParameters += this@klass.typeParameters.dropLast(1).map { typeParameter ->
                                    FirValueParameterImpl(
                                        this@FirLibrarySymbolProviderImpl.session,
                                        null,
                                        Name.identifier(typeParameter.name.asString().toLowerCase()),
                                        FirResolvedTypeRefImpl(
                                            null,
                                            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag(), false)
                                        ),
                                        defaultValue = null,
                                        isCrossinline = false,
                                        isNoinline = false,
                                        isVararg = false
                                    )
                                }
                            }
                        )
                        replaceSupertypes(listOf(session.builtinTypes.anyType))
                    }
                }
            }
        }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
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
        return getClassDeclarations(classId).filterIsInstance<FirCallableMemberDeclaration<*>>().mapTo(mutableSetOf()) { it.name }
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
