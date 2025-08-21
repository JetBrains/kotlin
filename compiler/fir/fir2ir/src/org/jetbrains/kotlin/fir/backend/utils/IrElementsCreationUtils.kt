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
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildFile
import org.jetbrains.kotlin.fir.declarations.utils.fileNameForPluginGeneratedCallable
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.name.*

context(c: Fir2IrComponents)
internal fun IrDeclarationParent.declareThisReceiverParameter(
    thisType: IrType,
    thisOrigin: IrDeclarationOrigin,
    kind: IrParameterKind,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    name: Name = SpecialNames.THIS,
    explicitReceiver: FirReceiverParameter? = null,
    isAssignable: Boolean = false
): IrValueParameter {
    return IrFactoryImpl.createValueParameter(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = thisOrigin,
        kind = kind,
        name = name,
        type = thisType,
        isAssignable = isAssignable,
        symbol = IrValueParameterSymbolImpl(),
        varargElementType = null,
        isCrossinline = false,
        isNoinline = false,
        isHidden = false,
    ).apply {
        this.parent = this@declareThisReceiverParameter
        explicitReceiver?.let { c.annotationGenerator.generate(this, it) }
    }
}

context(c: Fir2IrComponents)
internal fun IrClass.setThisReceiver(typeParameters: List<FirTypeParameterRef>) {
    val typeArguments = typeParameters.map {
        val typeParameter = c.classifierStorage.getIrTypeParameterSymbol(it.symbol, ConversionTypeOrigin.DEFAULT)
        IrSimpleTypeImpl(typeParameter, hasQuestionMark = false, emptyList(), emptyList())
    }
    thisReceiver = declareThisReceiverParameter(
        kind = IrParameterKind.DispatchReceiver,
        thisType = IrSimpleTypeImpl(symbol, false, typeArguments, emptyList()),
        thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
    )
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
        statements += createWhenForSafeFall(resultType, receiverVariableSymbol, expressionOnNotNull)
    }
}

fun Fir2IrComponents.createWhenForSafeFall(
    resultType: IrType,
    receiverVariableSymbol: IrValueSymbol,
    expressionOnNotNull: IrExpression,
): IrWhenImpl {
    val startOffset = expressionOnNotNull.startOffset
    val endOffset = expressionOnNotNull.endOffset

    return IrWhenImpl(startOffset, endOffset, resultType).apply {
        val condition = IrCallImplWithShape(
            startOffset, endOffset, builtins.booleanType,
            builtins.eqeqSymbol,
            valueArgumentsCount = 2,
            typeArgumentsCount = 0,
            contextParameterCount = 0,
            hasDispatchReceiver = false,
            hasExtensionReceiver = false,
            origin = IrStatementOrigin.EQEQ
        ).apply {
            arguments[0] = IrGetValueImpl(startOffset, endOffset, receiverVariableSymbol)
            arguments[1] = IrConstImpl.constNull(startOffset, endOffset, builtins.nothingNType)
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
    require((klass.valueClassRepresentation != null) == klass.isInlineOrValue) {
        "Value class has no representation: ${klass.render()}"
    }
    return klass.valueClassRepresentation?.mapUnderlyingType {
        it.toIrType() as? IrSimpleType ?: error("Value class underlying type is not a simple type: ${klass.render()}")
    }
}

@OptIn(FirExtensionApiInternals::class)
fun FirSession.createFilesWithGeneratedDeclarations(): List<FirFile> {
    val symbolProvider = generatedDeclarationsSymbolProvider ?: return emptyList()
    val declarationGenerators = extensionService.declarationGenerators

    val fileModuleData = this@createFilesWithGeneratedDeclarations.moduleData

    return buildList {
        val generatedClasses = declarationGenerators.flatMap { it.topLevelClassIdsCache.getValue() }
            .mapNotNull { symbolProvider.getClassLikeSymbolByClassId(it)?.fir }

        // classes go to a specific file each
        for (firClass in generatedClasses) {
            val classId = firClass.symbol.classId
            this += createSyntheticFile(
                fileName = "${classId.packageFqName.toPath()}/${classId.relativeClassName.asString()}.kt",
                packageFqName = classId.packageFqName,
                fileModuleData,
                FirDeclarationOrigin.Synthetic.PluginFile,
            ) {
                declarations += firClass
            }
        }

        // callables are grouped per package
        val generatedCallables = declarationGenerators.flatMap { it.topLevelCallableIdsCache.getValue() }
            .flatMap { symbolProvider.getTopLevelCallableSymbols(it.packageName, it.callableName) }

        val generatedCallablesPerPackage = generatedCallables.groupBy { it.callableId.packageName }
        for ((packageName, packageGeneratedCallables) in generatedCallablesPerPackage) {
            val callablesPerFileName = packageGeneratedCallables.groupBy {
                val name = it.fir.fileNameForPluginGeneratedCallable ?: "__GENERATED__CALLABLES__.kt"
                if (name.endsWith(".kt")) name else "$name.kt"
            }
            for ((fileName, callables) in callablesPerFileName) {
                this += createSyntheticFile(
                    fileName = "${packageName.toPath()}/$fileName",
                    packageFqName = packageName,
                    fileModuleData,
                    FirDeclarationOrigin.Synthetic.PluginFile,
                ) {
                    declarations += callables.map { it.fir }
                }
            }
        }
    }
}

private fun FqName.toPath(): String = this.asString().replace('.', '/')

const val generatedBuiltinsDeclarationsFileName: String = "__GENERATED BUILTINS DECLARATIONS__.kt"

fun FirSession.createFilesWithBuiltinsSyntheticDeclarationsIfNeeded(): List<FirFile> {
    // Check `dependsOnDependencies` to avoid generating duplicated declarations (if HMPP project structure is used)
    if (!languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) ||
        !moduleData.isCommon ||
        moduleData.dependsOnDependencies.isNotEmpty()
    ) {
        return emptyList()
    }
    val symbolProvider = syntheticFunctionInterfacesSymbolProvider

    return symbolProvider.generatedClassIds.groupBy { it.packageFqName }.map { (packageFqName, classIds) ->
        createSyntheticFile(
            fileName = generatedBuiltinsDeclarationsFileName,
            packageFqName = packageFqName,
            fileModuleData = moduleData,
            fileOrigin = FirDeclarationOrigin.Synthetic.Builtins,
        ) {
            declarations += classIds.mapNotNull { symbolProvider.getClassLikeSymbolByClassId(it)?.fir }
        }
    }
}

private fun createSyntheticFile(
    fileName: String,
    packageFqName: FqName,
    fileModuleData: FirModuleData,
    fileOrigin: FirDeclarationOrigin,
    init: FirFileBuilder.() -> Unit,
): FirFile {
    return buildFile {
        origin = fileOrigin
        moduleData = fileModuleData
        packageDirective = buildPackageDirective {
            this.packageFqName = packageFqName
        }
        name = fileName
        init()
    }
}

fun Fir2IrComponents.constTrue(startOffset: Int, endOffset: Int): IrConst {
    return IrConstImpl.constTrue(startOffset, endOffset, builtins.booleanType)
}

fun Fir2IrComponents.constFalse(startOffset: Int, endOffset: Int): IrConst {
    return IrConstImpl.constFalse(startOffset, endOffset, builtins.booleanType)
}

fun Fir2IrComponents.elseBranch(elseExpr: IrExpression): IrElseBranch {
    val startOffset = elseExpr.startOffset
    val endOffset = elseExpr.endOffset
    return IrElseBranchImpl(startOffset, endOffset, constTrue(startOffset, endOffset), elseExpr)
}
