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
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.deserialization.FirBuiltinAnnotationDeserializer
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.deserializeClassToSymbol
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getOrPut
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
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

class FirLibrarySymbolProviderImpl(val session: FirSession, val kotlinScopeProvider: KotlinScopeProvider) : FirSymbolProvider() {
    private class BuiltInsPackageFragment(
        stream: InputStream, val fqName: FqName, val session: FirSession,
        val kotlinScopeProvider: KotlinScopeProvider
    ) {
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

        val lookup = mutableMapOf<ClassId, FirRegularClassSymbol>()

        fun getClassLikeSymbolByFqName(classId: ClassId): FirRegularClassSymbol? =
            findAndDeserializeClass(classId)

        private fun findAndDeserializeClass(
            classId: ClassId,
            parentContext: FirDeserializationContext? = null
        ): FirRegularClassSymbol? {
            val classIdExists = classId in classDataFinder.allClassIds
            if (!classIdExists) return null
            return lookup.getOrPut(classId, { FirRegularClassSymbol(classId) }) { symbol ->
                val classData = classDataFinder.findClassData(classId)!!
                val classProto = classData.classProto

                deserializeClassToSymbol(
                    classId, classProto, symbol, nameResolver, session,
                    null, kotlinScopeProvider, parentContext,
                    this::findAndDeserializeClass
                )
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
            BuiltInsPackageFragment(inputStream, fqName, session, kotlinScopeProvider)
        }
    }

    private val allPackageFragments = loadBuiltIns().groupBy { it.fqName }

    private val fictitiousFunctionSymbols = mutableMapOf<Int, FirRegularClassSymbol>()

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirRegularClassSymbol? {
        return allPackageFragments[classId.packageFqName]?.firstNotNullResult {
            it.getClassLikeSymbolByFqName(classId)
        } ?: with(classId) {
            val className = relativeClassName.asString()
            val kind = FunctionClassDescriptor.Kind.byClassNamePrefix(packageFqName, className) ?: return@with null
            val prefix = kind.classNamePrefix
            val arity = className.substring(prefix.length).toIntOrNull() ?: return null
            fictitiousFunctionSymbols.getOrPut(arity) {
                val status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.ABSTRACT).apply {
                    isExpect = false
                    isActual = false
                    isInner = false
                    isCompanion = false
                    isData = false
                    isInline = false
                }
                FirRegularClassSymbol(this).apply {
                    FirClassImpl(
                        null,
                        session,
                        relativeClassName.shortName(),
                        status,
                        ClassKind.INTERFACE,
                        kotlinScopeProvider,
                        this
                    ).apply klass@{
                        resolvePhase = FirResolvePhase.DECLARATIONS
                        typeParameters.addAll((1..arity).map {
                            FirTypeParameterImpl(
                                null,
                                this@FirLibrarySymbolProviderImpl.session,
                                Name.identifier("P$it"),
                                FirTypeParameterSymbol(),
                                Variance.IN_VARIANCE,
                                false
                            ).apply {
                                bounds += session.builtinTypes.nullableAnyType
                            }
                        })
                        typeParameters.add(
                            FirTypeParameterImpl(
                                null,
                                this@FirLibrarySymbolProviderImpl.session,
                                Name.identifier("R"),
                                FirTypeParameterSymbol(),
                                Variance.OUT_VARIANCE,
                                false
                            ).apply {
                                bounds += session.builtinTypes.nullableAnyType
                            }
                        )
                        val name = OperatorNameConventions.INVOKE
                        val status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.ABSTRACT).apply {
                            isExpect = false
                            isActual = false
                            isOverride = false
                            isOperator = true
                            isInfix = false
                            isInline = false
                            isTailRec = false
                            isExternal = false
                            isSuspend = false
                        }
                        addDeclaration(
                            FirSimpleFunctionImpl(
                                null,
                                this@FirLibrarySymbolProviderImpl.session,
                                FirResolvedTypeRefImpl(
                                    null,
                                    ConeTypeParameterTypeImpl(
                                        typeParameters.last().symbol.toLookupTag(),
                                        false
                                    )
                                ),
                                null,
                                name,
                                status,
                                FirNamedFunctionSymbol(CallableId(packageFqName, relativeClassName, name))
                            ).apply {
                                resolvePhase = FirResolvePhase.DECLARATIONS
                                valueParameters += this@klass.typeParameters.dropLast(1).map { typeParameter ->
                                    val name = Name.identifier(typeParameter.name.asString().toLowerCase())
                                    FirValueParameterImpl(
                                        null,
                                        this@FirLibrarySymbolProviderImpl.session,
                                        FirResolvedTypeRefImpl(
                                            null,
                                            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag(), false)
                                        ),
                                        name,
                                        FirVariableSymbol(name),
                                        defaultValue = null,
                                        isCrossinline = false,
                                        isNoinline = false,
                                        isVararg = false
                                    )
                                }
                            }
                        )
                        replaceSuperTypeRefs(listOf(session.builtinTypes.anyType))
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

    override fun getNestedClassifierScope(classId: ClassId): FirScope? {
        return findRegularClass(classId)?.let {
            nestedClassifierScope(it)
        }
    }

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
        return getClassDeclarations(classId).filterIsInstance<FirMemberDeclaration>().mapTo(mutableSetOf()) { it.name }
    }

    private fun getClassDeclarations(classId: ClassId): List<FirDeclaration> {
        return findRegularClass(classId)?.declarations ?: emptyList()
    }


    private fun findRegularClass(classId: ClassId): FirRegularClass? =
        getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass

    override fun getNestedClassesNamesInClass(classId: ClassId): Set<Name> {
        return getClassDeclarations(classId).filterIsInstance<FirRegularClass>().mapTo(mutableSetOf()) { it.name }
    }
}
