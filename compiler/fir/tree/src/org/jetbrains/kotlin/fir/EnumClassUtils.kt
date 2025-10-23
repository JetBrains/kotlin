/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_ENTRIES
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUES
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUE_OF
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildNamedFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusWithLazyEffectiveVisibility
import org.jetbrains.kotlin.fir.expressions.builder.buildEmptyExpressionBlock
import org.jetbrains.kotlin.fir.symbols.id.symbolIdFactory
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

// Note: The fake source elements of the generated enum members should be distinct per the contract of `KtFakeSourceElementKind`. Hence, the
// generator functions must ensure that each pair of `(realSource, fakeElementKind)` is distinct.

fun FirRegularClassBuilder.generateValuesFunction(
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source,
) {
    declarations += generateValuesFunction(
        symbol,
        source,
        status,
        resolvePhase,
        moduleData,
        packageFqName,
        classFqName,
        makeExpect,
        origin,
    )
}

fun generateValuesFunction(
    classSymbol: FirRegularClassSymbol,
    classSource: KtSourceElement?,
    classStatus: FirDeclarationStatus,
    classResolvePhase: FirResolvePhase,
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source,
): FirNamedFunction {
    return buildNamedFunction {
        val sourceElement = classSource?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration.EnumValuesFunction)

        source = sourceElement
        this.origin = origin
        this.moduleData = moduleData

        val returnTypeRef = buildResolvedTypeRef {
            source = classSource?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration.EnumValuesFunctionReturnType)
            coneType = ConeClassLikeTypeImpl(
                StandardClassIds.Array.toLookupTag(),
                arrayOf(
                    ConeClassLikeTypeImpl(
                        classSymbol.toLookupTag(),
                        ConeTypeProjection.EMPTY_ARRAY,
                        isMarkedNullable = false
                    )
                ),
                isMarkedNullable = false
            )
        }
        this.returnTypeRef = returnTypeRef

        name = ENUM_VALUES
        this.status = createStatus(classStatus).apply {
            isStatic = true
            isExpect = makeExpect
        }
        isLocal = classStatus.visibility == Visibilities.Local

        symbol = FirNamedFunctionSymbol(
            // Some users of `generateValuesFunction` don't have sources, such as FIR metadata class deserialization.
            moduleData.session.symbolIdFactory.sourceBasedOrUnique(sourceElement),
            CallableId(packageFqName, classFqName, ENUM_VALUES)
        )

        resolvePhase = classResolvePhase
        body = buildEmptyExpressionBlock().also {
            it.replaceConeTypeOrNull(returnTypeRef.coneType)
        }
    }.apply {
        containingClassForStaticMemberAttr = classSymbol.toLookupTag()
    }
}

fun FirRegularClassBuilder.generateValueOfFunction(
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source,
) {
    declarations += generateValueOfFunction(
        symbol,
        source,
        status,
        resolvePhase,
        moduleData,
        packageFqName,
        classFqName,
        makeExpect,
        origin,
    )
}

fun generateValueOfFunction(
    classSymbol: FirRegularClassSymbol,
    classSource: KtSourceElement?,
    classStatus: FirDeclarationStatus,
    classResolvePhase: FirResolvePhase,
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source,
): FirNamedFunction {
    val symbolIdFactory = moduleData.session.symbolIdFactory
    val sourceElement = classSource?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration.EnumValueOfFunction)

    return buildNamedFunction {
        source = sourceElement
        this.origin = origin
        this.moduleData = moduleData
        val returnTypeRef = buildResolvedTypeRef {
            source = sourceElement
            coneType = ConeClassLikeTypeImpl(
                classSymbol.toLookupTag(),
                emptyArray(),
                isMarkedNullable = false
            )
        }
        this.returnTypeRef = returnTypeRef
        name = ENUM_VALUE_OF

        status = createStatus(classStatus).apply {
            isStatic = true
            isExpect = makeExpect
        }
        isLocal = classStatus.visibility == Visibilities.Local
        symbol = FirNamedFunctionSymbol(
            // Some users of `generateValueOfFunction` don't have sources, such as FIR metadata class deserialization.
            symbolIdFactory.sourceBasedOrUnique(sourceElement),
            CallableId(packageFqName, classFqName, ENUM_VALUE_OF),
        )
        valueParameters += buildValueParameter vp@{
            val valueParameterSource = classSource
                ?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration.EnumValueOfFunctionParameter)

            source = valueParameterSource
            containingDeclarationSymbol = this@buildNamedFunction.symbol
            this.origin = origin
            this.moduleData = moduleData
            this.returnTypeRef = buildResolvedTypeRef {
                source = classSource?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration.EnumValueOfFunctionReturnType)
                coneType = ConeClassLikeTypeImpl(
                    StandardClassIds.String.toLookupTag(),
                    emptyArray(),
                    isMarkedNullable = false
                )
            }
            name = DEFAULT_VALUE_PARAMETER
            this@vp.symbol = FirValueParameterSymbol(symbolIdFactory.sourceBasedOrUnique(valueParameterSource))
            isCrossinline = false
            isNoinline = false
            isVararg = false
            resolvePhase = classResolvePhase
        }
        resolvePhase = classResolvePhase
        body = buildEmptyExpressionBlock().also {
            it.replaceConeTypeOrNull(returnTypeRef.coneType)
        }
    }.apply {
        containingClassForStaticMemberAttr = classSymbol.toLookupTag()
    }
}

fun FirRegularClassBuilder.generateEntriesGetter(
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source,
) {
    declarations += generateEntriesGetter(
        symbol,
        source,
        status,
        resolvePhase,
        moduleData,
        packageFqName,
        classFqName,
        makeExpect,
        origin,
    )
}

fun generateEntriesGetter(
    classSymbol: FirRegularClassSymbol,
    classSource: KtSourceElement?,
    classStatus: FirDeclarationStatus,
    classResolvePhase: FirResolvePhase,
    moduleData: FirModuleData,
    packageFqName: FqName,
    classFqName: FqName,
    makeExpect: Boolean = false,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Source,
): FirProperty {
    val symbolIdFactory = moduleData.session.symbolIdFactory

    return buildProperty {
        val sourceElement = classSource?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration.EnumEntriesProperty)

        source = sourceElement
        isVar = false
        this.origin = origin
        this.moduleData = moduleData
        returnTypeRef = buildResolvedTypeRef {
            source = classSource?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration.EnumEntriesPropertyReturnType)
            coneType = ConeClassLikeTypeImpl(
                StandardClassIds.EnumEntries.toLookupTag(),
                arrayOf(
                    ConeClassLikeTypeImpl(
                        classSymbol.toLookupTag(),
                        ConeTypeProjection.EMPTY_ARRAY,
                        isMarkedNullable = false
                    )
                ),
                isMarkedNullable = false
            )
        }
        name = ENUM_ENTRIES
        this.status = createStatus(classStatus).apply {
            isStatic = true
            isExpect = makeExpect
        }
        isLocal = classStatus.visibility == Visibilities.Local

        symbol = FirRegularPropertySymbol(
            symbolIdFactory.sourceBasedOrUnique(sourceElement),
            CallableId(packageFqName, classFqName, ENUM_ENTRIES),
        )
        resolvePhase = classResolvePhase

        val getterTypeRefSource =
            returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.EnumGeneratedDeclaration.EnumEntriesPropertyGetterReturnType)

        val propertyGetterSource = classSource?.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration.EnumEntriesPropertyGetter)
        getter = FirDefaultPropertyGetter(
            source = propertyGetterSource,
            moduleData = moduleData,
            origin = origin,
            propertyTypeRef = getterTypeRefSource,
            visibility = Visibilities.Public,
            symbol = FirPropertyAccessorSymbol(symbolIdFactory.sourceBasedOrUnique(propertyGetterSource)),
            propertySymbol = symbol,
            modality = Modality.FINAL,
            resolvePhase = classResolvePhase,
        ).apply {
            this.status = createStatus(classStatus).apply {
                isStatic = true
            }
        }
    }.apply {
        containingClassForStaticMemberAttr = classSymbol.toLookupTag()
    }
}

@OptIn(FirImplementationDetail::class)
private fun createStatus(parentStatus: FirDeclarationStatus): FirDeclarationStatusImpl {
    return when (parentStatus) {
        is FirResolvedDeclarationStatusImpl -> FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.FINAL,
            parentStatus.effectiveVisibility
        )
        is FirResolvedDeclarationStatusWithLazyEffectiveVisibility -> FirResolvedDeclarationStatusWithLazyEffectiveVisibility(
            Visibilities.Public,
            Modality.FINAL,
            parentStatus.lazyEffectiveVisibility,
        )
        else -> FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
    }
}
