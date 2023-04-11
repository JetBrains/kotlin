/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private val FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME = StandardClassIds.Annotations.FlexibleNullability.asSingleFqName()

internal fun Map<String, List<IrDeclaration>>.getMatches(
    expectDeclaration: IrDeclaration,
    expectActualTypesMap: Map<IrSymbol, IrSymbol>,
    expectActualTypeAliasMap: Map<FqName, FqName>
): List<IrDeclaration> =
    getMatches(generateIrElementFullNameFromExpect(expectDeclaration, expectActualTypeAliasMap), expectDeclaration, expectActualTypesMap)

internal fun Map<String, List<IrDeclaration>>.getMatches(
    mainSignature: String,
    expectDeclaration: IrDeclaration,
    expectActualTypesMap: Map<IrSymbol, IrSymbol>
): List<IrDeclaration> {
    val members = this[mainSignature] ?: return emptyList()
    return when (expectDeclaration) {
        is IrFunction -> members.getMatches(expectDeclaration, expectActualTypesMap) { it as IrFunction }
        is IrProperty -> members.getMatches(expectDeclaration, expectActualTypesMap) { (it as IrProperty).getter!! }
        else -> members
    }
}

private inline fun List<IrDeclaration>.getMatches(
    expect: IrDeclaration,
    expectActualTypesMap: Map<IrSymbol, IrSymbol>,
    functionExtractor: (IrDeclaration) -> IrFunction
): List<IrDeclaration> {
    val expectFunction = functionExtractor(expect)
    return filter { expectFunction.match(functionExtractor(it), expectActualTypesMap) }
}

private fun IrFunction.match(actualFunction: IrFunction, expectActualTypesMap: Map<IrSymbol, IrSymbol>): Boolean {
    fun getActualizedTypeClassifierSymbol(
        expectParameter: IrValueParameter,
        localTypeParametersMap: Map<IrTypeParameterSymbol, IrTypeParameterSymbol>? = null
    ): IrSymbol {
        return expectParameter.type.classifierOrFail.let {
            val localMappedSymbol = if (localTypeParametersMap != null && it is IrTypeParameterSymbol) {
                localTypeParametersMap[it]
            } else {
                null
            }
            localMappedSymbol ?: expectActualTypesMap[it] ?: it
        }
    }

    fun checkParameter(
        expectParameter: IrValueParameter?,
        actualParameter: IrValueParameter?,
        localTypeParametersMap: Map<IrTypeParameterSymbol, IrTypeParameterSymbol>
    ): Boolean {
        if (expectParameter == null) {
            return actualParameter == null
        }
        if (actualParameter == null) {
            return false
        }

        if (expectParameter.type is IrDynamicType || actualParameter.type is IrDynamicType) {
            return true
        }

        if (expectParameter.type.isNullable() != actualParameter.type.isNullable() &&
            !expectParameter.type.annotations.hasAnnotation(FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME) &&
            !actualParameter.type.annotations.hasAnnotation(FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME)
        ) {
            return false
        }

        if (getActualizedTypeClassifierSymbol(expectParameter, localTypeParametersMap) !=
            getActualizedTypeClassifierSymbol(actualParameter)
        ) {
            return false
        }

        return true
    }

    if (valueParameters.size != actualFunction.valueParameters.size || typeParameters.size != actualFunction.typeParameters.size) {
        return false
    }

    val localTypeParametersMap = mutableMapOf<IrTypeParameterSymbol, IrTypeParameterSymbol>()
    for ((expectTypeParameter, actualTypeParameter) in typeParameters.zip(actualFunction.typeParameters)) {
        if (expectTypeParameter.name != actualTypeParameter.name) {
            return false
        }
        localTypeParametersMap[expectTypeParameter.symbol] = actualTypeParameter.symbol
    }

    if (!checkParameter(extensionReceiverParameter, actualFunction.extensionReceiverParameter, localTypeParametersMap)) {
        return false
    }

    for ((expectParameter, actualParameter) in valueParameters.zip(actualFunction.valueParameters)) {
        if (!checkParameter(expectParameter, actualParameter, localTypeParametersMap)) {
            return false
        }
    }

    return true
}

internal fun generateActualIrClassOrTypeAliasFullName(declaration: IrElement) = generateIrElementFullNameFromExpect(declaration, emptyMap())

internal fun generateIrElementFullNameFromExpect(
    declaration: IrElement,
    expectActualTypeAliasMap: Map<FqName, FqName>
): String {
    return buildString { appendElementFullName(declaration, this, expectActualTypeAliasMap) }
}

private fun appendElementFullName(
    declaration: IrElement,
    result: StringBuilder,
    expectActualTypeAliasMap: Map<FqName, FqName>
) {
    if (declaration !is IrDeclarationBase) return

    val parents = mutableListOf<String>()
    var parent: IrDeclarationParent? = declaration.parent
    while (parent != null) {
        if (parent is IrDeclarationWithName) {
            val parentParent = parent.parent
            if (parentParent is IrClass) {
                parents.add(parent.name.asString())
                parent = parentParent
                continue
            }
        }
        val parentString = parent.kotlinFqName.let { (expectActualTypeAliasMap[it] ?: it).asString() }
        if (parentString.isNotEmpty()) {
            parents.add(parentString)
        }
        parent = null
    }

    if (parents.isNotEmpty()) {
        result.append(parents.asReversed().joinToString(separator = "."))
        result.append('.')
    }

    if (declaration is IrDeclarationWithName) {
        result.append(declaration.name)
    }

    if (declaration is IrFunction) {
        result.append("()")
    }
}

internal fun MutableMap<IrSymbol, IrSymbol>.addLink(expectMember: IrDeclarationBase, actualMember: IrDeclaration) {
    this[expectMember.symbol] = actualMember.symbol
    if (expectMember is IrProperty) {
        val actualProperty = actualMember as IrProperty
        expectMember.getter!!.let {
            val getter = actualProperty.getter!!
            this[it.symbol] = getter.symbol
            this.appendTypeParametersMap(it, getter)
        }
        expectMember.setter?.symbol?.let { this[it] = actualProperty.setter!!.symbol }
    } else if (expectMember is IrFunction) {
        this.appendTypeParametersMap(expectMember, actualMember as IrFunction)
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun KtDiagnosticReporterWithImplicitIrBasedContext.reportMissingActual(irDeclaration: IrDeclaration) {
    at(irDeclaration).report(
        CommonBackendErrors.NO_ACTUAL_FOR_EXPECT,
        (irDeclaration as? IrDeclarationWithName)?.name?.asString().orEmpty(),
        irDeclaration.module
    )
}

internal fun IrElement.containsOptionalExpectation(): Boolean {
    return this is IrClass &&
            this.kind == ClassKind.ANNOTATION_CLASS &&
            this.hasAnnotation(OptionalAnnotationUtil.OPTIONAL_EXPECTATION_FQ_NAME)
}

@Suppress("UNCHECKED_CAST")
internal fun createFakeOverrideMember(actualMembers: List<IrDeclaration>, declaration: IrClass): IrOverridableDeclaration<*> {
    return when (actualMembers.first()) {
        is IrSimpleFunction -> createFakeOverrideFunction(actualMembers as List<IrSimpleFunction>, declaration)
        is IrProperty -> createFakeOverrideProperty(actualMembers as List<IrProperty>, declaration)
        else -> error("Only function or property can be overridden")
    }
}

private fun createFakeOverrideProperty(actualProperties: List<IrProperty>, parent: IrClass): IrProperty {
    val actualProperty = actualProperties.first() // TODO: Currently FIR2IR works in the similar way but it looks incorrect
    return parent.factory.buildProperty {
        updateFrom(actualProperty)
        name = actualProperty.name
        origin = IrDeclarationOrigin.FAKE_OVERRIDE
        isFakeOverride = true
        isExpect = false
    }.apply {
        this.parent = parent
        annotations = actualProperty.annotations
        backingField = null
        getter = createFakeOverrideFunction(actualProperties.map { it.getter as IrSimpleFunction }, parent, symbol)
        setter = runIf(actualProperty.setter != null) {
            createFakeOverrideFunction(actualProperties.map { it.setter as IrSimpleFunction }, parent, symbol)
        }
        overriddenSymbols = actualProperties.map { it.symbol }
    }
}

private fun createFakeOverrideFunction(
    actualFunctions: List<IrSimpleFunction>,
    parent: IrDeclarationParent,
    correspondingPropertySymbol: IrPropertySymbol? = null
): IrSimpleFunction {
    val actualFunction = actualFunctions.first() // TODO: Currently FIR2IR works in the similar way but it looks incorrect

    return actualFunction.factory.buildFun {
        updateFrom(actualFunction)
        name = actualFunction.name
        returnType = actualFunction.returnType
        origin = IrDeclarationOrigin.FAKE_OVERRIDE
        isFakeOverride = true
        isExpect = false
    }.also {
        it.parent = parent
        it.annotations = actualFunction.annotations.map { p -> p.deepCopyWithSymbols(it) }
        it.typeParameters = actualFunction.typeParameters.map { p -> p.deepCopyWithSymbols(it) }

        val typeRemapper = IrTypeParameterRemapper(actualFunction.typeParameters.zip(it.typeParameters).toMap())
        fun IrValueParameter.deepCopyWithTypeParameters(): IrValueParameter = deepCopyWithSymbols(it) { symbolRemapper, _ ->
            DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper)
        }

        it.dispatchReceiverParameter = actualFunction.dispatchReceiverParameter?.deepCopyWithTypeParameters()
        it.extensionReceiverParameter = actualFunction.extensionReceiverParameter?.deepCopyWithTypeParameters()
        it.valueParameters = actualFunction.valueParameters.map { p -> p.deepCopyWithTypeParameters() }
        it.contextReceiverParametersCount = actualFunction.contextReceiverParametersCount
        it.metadata = actualFunction.metadata
        it.overriddenSymbols = actualFunctions.map { f -> f.symbol }
        it.attributeOwnerId = it
        it.correspondingPropertySymbol = correspondingPropertySymbol
    }
}