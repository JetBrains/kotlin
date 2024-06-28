/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildFile
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirStdlibBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.name.*

internal fun IrDeclarationParent.declareThisReceiverParameter(
    c: Fir2IrComponents,
    thisType: IrType,
    thisOrigin: IrDeclarationOrigin,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    name: Name = SpecialNames.THIS,
    explicitReceiver: FirReceiverParameter? = null,
    isAssignable: Boolean = false
): IrValueParameter {
    return c.irFactory.createValueParameter(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = thisOrigin,
        name = name,
        type = thisType,
        isAssignable = isAssignable,
        symbol = IrValueParameterSymbolImpl(),
        index = UNDEFINED_PARAMETER_INDEX,
        varargElementType = null,
        isCrossinline = false,
        isNoinline = false,
        isHidden = false,
    ).apply {
        this.parent = this@declareThisReceiverParameter
        explicitReceiver?.let { c.annotationGenerator.generate(this, it) }
    }
}

fun Fir2IrComponents.createSafeCallConstruction(
    receiverVariable: IrVariable,
    receiverVariableSymbol: IrValueSymbol,
    expressionOnNotNull: IrExpression,
): IrExpression {
    val startOffset = expressionOnNotNull.startOffset
    val endOffset = expressionOnNotNull.endOffset

    val resultType = expressionOnNotNull.type.makeNullable()
    return IrBlockImpl(startOffset, endOffset, resultType, IrStatementOrigin.SAFE_CALL).apply {
        statements += receiverVariable
        statements += IrWhenImpl(startOffset, endOffset, resultType).apply {
            val condition = IrCallImpl(
                startOffset, endOffset, builtins.booleanType,
                builtins.eqeqSymbol,
                valueArgumentsCount = 2,
                typeArgumentsCount = 0,
                origin = IrStatementOrigin.EQEQ
            ).apply {
                putValueArgument(0, IrGetValueImpl(startOffset, endOffset, receiverVariableSymbol))
                putValueArgument(1, IrConstImpl.constNull(startOffset, endOffset, builtins.nothingNType))
            }
            branches += IrBranchImpl(
                condition, IrConstImpl.constNull(startOffset, endOffset, builtins.nothingNType)
            )
            branches += IrElseBranchImpl(
                IrConstImpl.boolean(startOffset, endOffset, builtins.booleanType, true),
                expressionOnNotNull
            )
        }
    }
}

fun Fir2IrConversionScope.createTemporaryVariable(
    receiverExpression: IrExpression,
    nameHint: String? = null
): Pair<IrVariable, IrValueSymbol> {
    val receiverVariable = scope().createTemporaryVariable(receiverExpression, nameHint)
    val variableSymbol = receiverVariable.symbol

    return Pair(receiverVariable, variableSymbol)
}

fun Fir2IrConversionScope.createTemporaryVariableForSafeCallConstruction(
    receiverExpression: IrExpression
): Pair<IrVariable, IrValueSymbol> =
    createTemporaryVariable(receiverExpression, "safe_receiver")

fun Fir2IrComponents.computeValueClassRepresentation(klass: FirRegularClass): ValueClassRepresentation<IrSimpleType>? {
    require((klass.valueClassRepresentation != null) == klass.isInline) {
        "Value class has no representation: ${klass.render()}"
    }
    return klass.valueClassRepresentation?.mapUnderlyingType {
        it.toIrType(this) as? IrSimpleType ?: error("Value class underlying type is not a simple type: ${klass.render()}")
    }
}

@OptIn(FirExtensionApiInternals::class)
fun FirSession.createFilesWithGeneratedDeclarations(): List<FirFile> {
    val symbolProvider = generatedDeclarationsSymbolProvider ?: return emptyList()
    val declarationGenerators = extensionService.declarationGenerators

    return createSyntheticFiles(
        this@createFilesWithGeneratedDeclarations.moduleData,
        "__GENERATED DECLARATIONS__.kt",
        FirDeclarationOrigin.Synthetic.PluginFile,
        symbolProvider,
        topLevelClasses = declarationGenerators.flatMap { it.topLevelClassIdsCache.getValue() }.groupBy { it.packageFqName },
        topLevelCallables = declarationGenerators.flatMap { it.topLevelCallableIdsCache.getValue() }.groupBy { it.packageName },
    )
}

const val generatedBuiltinsDeclarationsFileName: String = "__GENERATED BUILTINS DECLARATIONS__.kt"

fun FirSession.createFilesWithBuiltinsSyntheticDeclarationsIfNeeded(): List<FirFile> {
    // Check `dependsOnDependencies` to avoid generating duplicated declarations (if HMPP project structure is used)
    if (!languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) ||
        !moduleData.isCommon ||
        moduleData.dependsOnDependencies.isNotEmpty()
    ) {
        return emptyList()
    }
    val symbolProvider =
        (symbolProvider as FirCachingCompositeSymbolProvider).providers.filterIsInstance<FirStdlibBuiltinSyntheticFunctionInterfaceProvider>()
            .single()

    return createSyntheticFiles(
        this@createFilesWithBuiltinsSyntheticDeclarationsIfNeeded.moduleData,
        generatedBuiltinsDeclarationsFileName,
        FirDeclarationOrigin.Synthetic.Builtins,
        symbolProvider,
        topLevelClasses = symbolProvider.generatedClasses.map { it.classId }.groupBy { it.packageFqName },
        topLevelCallables = emptyMap(),
    )
}

private fun createSyntheticFiles(
    fileModuleData: FirModuleData,
    fileName: String,
    fileOrigin: FirDeclarationOrigin,
    symbolProvider: FirSymbolProvider,
    topLevelClasses: Map<FqName, List<ClassId>>,
    topLevelCallables: Map<FqName, List<CallableId>>,
): List<FirFile> {
    return buildList {
        for (packageFqName in (topLevelClasses.keys + topLevelCallables.keys)) {
            this += buildFile {
                origin = fileOrigin
                moduleData = fileModuleData
                packageDirective = buildPackageDirective {
                    this.packageFqName = packageFqName
                }
                name = fileName
                declarations += topLevelCallables.getOrDefault(packageFqName, emptyList())
                    .flatMap { symbolProvider.getTopLevelCallableSymbols(packageFqName, it.callableName) }
                    .map { it.fir }
                declarations += topLevelClasses.getOrDefault(packageFqName, emptyList())
                    .mapNotNull { symbolProvider.getClassLikeSymbolByClassId(it)?.fir }
            }
        }
    }
}

fun Fir2IrComponents.constTrue(startOffset: Int, endOffset: Int): IrConst<Boolean> {
    return IrConstImpl.constTrue(startOffset, endOffset, builtins.booleanType)
}

fun Fir2IrComponents.constFalse(startOffset: Int, endOffset: Int): IrConst<Boolean> {
    return IrConstImpl.constFalse(startOffset, endOffset, builtins.booleanType)
}

fun Fir2IrComponents.elseBranch(elseExpr: IrExpression): IrElseBranch {
    val startOffset = elseExpr.startOffset
    val endOffset = elseExpr.endOffset
    return IrElseBranchImpl(startOffset, endOffset, constTrue(startOffset, endOffset), elseExpr)
}
