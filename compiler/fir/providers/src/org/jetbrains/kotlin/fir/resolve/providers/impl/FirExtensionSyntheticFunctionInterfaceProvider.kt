/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isBuiltin
import org.jetbrains.kotlin.builtins.functions.isSuspendOrKSuspendFunction
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.addDeclaration
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

/*
 * Provides function interfaces for function kinds from compiler plugins
 */
class FirExtensionSyntheticFunctionInterfaceProvider(
    session: FirSession,
    moduleData: FirModuleData,
    kotlinScopeProvider: FirKotlinScopeProvider
) : FirSyntheticFunctionInterfaceProviderBase(session, moduleData, kotlinScopeProvider) {
    override fun FunctionTypeKind.isAcceptable(): Boolean {
        return !this.isBuiltin
    }
}

/*
 * Provides kotlin.FunctionN, kotlin.coroutines.SuspendFunctionN, kotlin.reflect.KFunctionN and kotlin.reflect.KSuspendFunctionN
 */
class FirBuiltinSyntheticFunctionInterfaceProvider(
    session: FirSession,
    moduleData: FirModuleData,
    kotlinScopeProvider: FirKotlinScopeProvider
) : FirSyntheticFunctionInterfaceProviderBase(session, moduleData, kotlinScopeProvider) {
    override fun FunctionTypeKind.isAcceptable(): Boolean {
        return this.isBuiltin
    }
}

abstract class FirSyntheticFunctionInterfaceProviderBase(
    session: FirSession,
    val moduleData: FirModuleData,
    val kotlinScopeProvider: FirKotlinScopeProvider
) : FirSymbolProvider(session) {
    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
        return cache.getValue(classId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    override fun getPackage(fqName: FqName): FqName? {
        return fqName.takeIf { session.functionTypeService.hasKindWithSpecificPackage(it) }
    }

    override fun computePackageSetWithTopLevelCallables(): Set<String> {
        return emptySet()
    }

    /**
     * This method has no sense for synthetic function interfaces
     */
    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? {
        return emptySet()
    }

    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? {
        return emptySet()
    }

    private val cache = moduleData.session.firCachesFactory.createCache(::createSyntheticFunctionInterface)

    protected abstract fun FunctionTypeKind.isAcceptable(): Boolean

    private fun createSyntheticFunctionInterface(classId: ClassId): FirRegularClassSymbol? {
        return with(classId) {
            val className = relativeClassName.asString()
            val kind = session.functionTypeService.getKindByClassNamePrefix(packageFqName, className) ?: return null
            if (!kind.isAcceptable()) return null
            val prefix = kind.classNamePrefix
            val arity = className.substring(prefix.length).toIntOrNull() ?: return null
            FirRegularClassSymbol(classId).apply symbol@{
                buildRegularClass klass@{
                    moduleData = this@FirSyntheticFunctionInterfaceProviderBase.moduleData
                    origin = FirDeclarationOrigin.BuiltIns
                    name = relativeClassName.shortName()
                    status = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.ABSTRACT,
                        EffectiveVisibility.Public
                    )
                    classKind = ClassKind.INTERFACE
                    scopeProvider = kotlinScopeProvider
                    symbol = this@symbol
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    typeParameters.addAll(
                        (1..arity).map {
                            buildTypeParameter {
                                moduleData = this@FirSyntheticFunctionInterfaceProviderBase.moduleData
                                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                                origin = FirDeclarationOrigin.BuiltIns
                                name = Name.identifier("P$it")
                                symbol = FirTypeParameterSymbol()
                                containingDeclarationSymbol = this@symbol
                                variance = Variance.IN_VARIANCE
                                isReified = false
                                bounds += moduleData.session.builtinTypes.nullableAnyType
                            }
                        },
                    )
                    typeParameters.add(
                        buildTypeParameter {
                            moduleData = this@FirSyntheticFunctionInterfaceProviderBase.moduleData
                            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                            origin = FirDeclarationOrigin.BuiltIns
                            name = Name.identifier("R")
                            symbol = FirTypeParameterSymbol()
                            containingDeclarationSymbol = this@symbol
                            variance = Variance.OUT_VARIANCE
                            isReified = false
                            bounds += moduleData.session.builtinTypes.nullableAnyType
                        },
                    )
                    val name = OperatorNameConventions.INVOKE
                    val functionStatus = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.ABSTRACT,
                        EffectiveVisibility.Public
                    ).apply {
                        isOperator = true
                        isSuspend = kind.isSuspendOrKSuspendFunction
                        hasStableParameterNames = false
                    }
                    val typeArguments = typeParameters.map {
                        ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false).toFirResolvedTypeRef()
                    }

                    fun createSuperType(kind: FunctionTypeKind): FirResolvedTypeRef {
                        return kind.classId(arity).toLookupTag()
                            .constructClassType(typeArguments.map { it.type }.toTypedArray(), isNullable = false)
                            .toFirResolvedTypeRef()
                    }

                    if (kind.isReflectType) {
                        superTypeRefs += StandardClassIds.KFunction.toLookupTag()
                            .constructClassType(arrayOf(typeArguments.last().type), isNullable = false)
                            .toFirResolvedTypeRef()
                        superTypeRefs += createSuperType(kind.nonReflectKind())
                    } else {
                        superTypeRefs += StandardClassIds.Function.toLookupTag()
                            .constructClassType(arrayOf(typeArguments.last().type), isNullable = false)
                            .toFirResolvedTypeRef()
                    }

                    addDeclaration(
                        buildSimpleFunction {
                            moduleData = this@FirSyntheticFunctionInterfaceProviderBase.moduleData
                            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                            origin = FirDeclarationOrigin.BuiltIns
                            returnTypeRef = typeArguments.last()
                            this.name = name
                            status = functionStatus
                            symbol = FirNamedFunctionSymbol(
                                CallableId(packageFqName, relativeClassName, name)
                            )
                            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                            valueParameters += typeArguments.dropLast(1).mapIndexed { index, typeArgument ->
                                val parameterName = Name.identifier("p${index + 1}")
                                buildValueParameter {
                                    moduleData = this@FirSyntheticFunctionInterfaceProviderBase.moduleData
                                    containingFunctionSymbol = this@buildSimpleFunction.symbol
                                    origin = FirDeclarationOrigin.BuiltIns
                                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                                    returnTypeRef = typeArgument
                                    this.name = parameterName
                                    symbol = FirValueParameterSymbol(parameterName)
                                    defaultValue = null
                                    isCrossinline = false
                                    isNoinline = false
                                    isVararg = false
                                }
                            }
                            dispatchReceiverType = classId.defaultType(this@klass.typeParameters.map { it.symbol })
                            kind.annotationOnInvokeClassId?.let { annotationClassId ->
                                annotations += buildAnnotation {
                                    annotationTypeRef = annotationClassId
                                        .constructClassLikeType(emptyArray(), isNullable = false)
                                        .toFirResolvedTypeRef()
                                    argumentMapping = FirEmptyAnnotationArgumentMapping
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun FunctionTypeKind.classId(arity: Int) = ClassId(packageFqName, numberedClassName(arity))
}
