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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil

internal fun Map<String, List<IrDeclaration>>.getMatches(
    expectDeclaration: IrDeclaration,
    expectActualTypesMap: Map<IrSymbol, IrSymbol>,
    expectActualTypeAliasMap: Map<FqName, FqName>
): List<IrDeclaration> {
    val members = this[generateIrElementFullNameFromExpect(expectDeclaration, expectActualTypeAliasMap)] ?: return emptyList()
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
    fun getActualizedValueParameterSymbol(
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

        if (getActualizedValueParameterSymbol(expectParameter, localTypeParametersMap) !=
            getActualizedValueParameterSymbol(actualParameter)
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

fun generateActualIrClassOrTypeAliasFullName(declaration: IrElement) = generateIrElementFullNameFromExpect(declaration, emptyMap())

fun generateIrElementFullNameFromExpect(
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

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun KtDiagnosticReporterWithImplicitIrBasedContext.reportMissingActual(irDeclaration: IrDeclaration) {
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
