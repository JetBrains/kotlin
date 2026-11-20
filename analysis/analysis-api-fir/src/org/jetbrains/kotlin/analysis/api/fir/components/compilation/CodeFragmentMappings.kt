/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.compilation

import org.jetbrains.kotlin.analysis.api.compile.KaCodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedId
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedValueAnalyzer
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.InlineLambdaArgument
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.utils.CodeFragmentConversionData
import org.jetbrains.kotlin.fir.backend.utils.InjectedValue
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

/**
 * @property capturedValues The list captured values.
 * Contains additional data required by the debugger in order to evaluate input parameters of evaluator-generated method.
 *
 * @property capturedFiles The list of files captured by the given code fragment or lambdas from [inlineLambdaParametersMapping]
 *
 * @property injectedValues The list captured values.
 * Contains all data required by FIR2IR conversion of captured values access.
 *
 * @property conversionData The data required for FIR2IR conversion of evaluator-generated class declaration.
 *
 * @property reifiedTypeParametersMapping The mapping of captured reified type parameters to theirs arguments.
 * Extracted from the execution stack via debugger extension point.
 *
 * @property inlineLambdaParametersMapping The mapping of inline lambda parameters to theirs arguments.
 * Extracted from the execution stack via debugger extension point.
 *
 * @property selfSymbols The set of symbols which declarations are located inside the given code fragment or inside lambdas presented in
 * [inlineLambdaParametersMapping]
 */
internal class CodeFragmentMappings(
    val capturedValues: List<KaCodeFragmentCapturedValue>,
    val capturedFiles: List<KtFile>,
    val injectedValues: List<InjectedValue>,
    val conversionData: CodeFragmentConversionData,
    val reifiedTypeParametersMapping: Map<FirTypeParameterSymbol, ConeKotlinType>,
    val inlineLambdaParametersMapping: Map<FirValueParameterSymbol, FirExpression>,
    val selfSymbols: Set<FirBasedSymbol<*>>,
)

@OptIn(LLFirInternals::class)
internal fun computeCodeFragmentMappings(
    mainFirFile: FirFile,
    resolutionFacade: LLResolutionFacade,
    codeFragmentClassName: String?,
    codeFragmentMethodName: String?,
    reifiedTypeParametersMapping: Map<FirTypeParameterSymbol, ConeKotlinType>,
    inlineLambdaParametersMapping: Map<FirValueParameterSymbol, InlineLambdaArgument>,
): CodeFragmentMappings {
    val codeFragment = mainFirFile.codeFragment

    val capturedData = CodeFragmentCapturedValueAnalyzer.analyze(resolutionFacade, codeFragment, inlineLambdaParametersMapping)

    val capturedSymbols = capturedData.symbols
    val capturedValues = capturedSymbols.map { it.value }
    val injectedValues = capturedSymbols.map { InjectedValue(it.symbol, it.typeRef, it.value.isMutated) }

    val conversionData = CodeFragmentConversionData(
        classId = ClassId(FqName.ROOT, Name.identifier(codeFragmentClassName ?: "CodeFragment")),
        methodName = Name.identifier(codeFragmentMethodName ?: "run"),
        injectedValues
    )

    return CodeFragmentMappings(
        capturedValues,
        capturedData.files,
        injectedValues,
        conversionData,
        reifiedTypeParametersMapping,
        inlineLambdaParametersMapping.mapValues { it.value.expr },
        capturedData.selfSymbols
    )
}

internal class InjectedSymbolProvider(
    codeFragmentMappings: CodeFragmentMappings,
    private val mainKtFile: KtFile,
) {
    val conversionData: CodeFragmentConversionData = codeFragmentMappings.conversionData

    private val injectedValueMapping: Map<CodeFragmentCapturedId, InjectedValue> =
        codeFragmentMappings.injectedValues.associateBy { CodeFragmentCapturedId(it.symbol) }

    fun invoke(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
        val irFile = conversionScope.containingFileIfAny()
        val psiFile = (irFile?.fileEntry as? PsiIrFileEntry)?.psiFile

        if (psiFile != mainKtFile) {
            return null
        }

        val id = when (calleeReference) {
            is FirThisReference -> when (val boundSymbol = calleeReference.boundSymbol) {
                is FirClassSymbol -> CodeFragmentCapturedId(boundSymbol)
                is FirReceiverParameterSymbol -> CodeFragmentCapturedId(boundSymbol)
                is FirTypeParameterSymbol, is FirTypeAliasSymbol -> errorWithFirSpecificEntries(
                    message = "Unexpected FirThisOwnerSymbol ${calleeReference::class.simpleName}", fir = boundSymbol.fir
                )
                null -> null
            }
            else -> calleeReference.toResolvedSymbol<FirBasedSymbol<*>>()?.let { CodeFragmentCapturedId(it) }
        }
        return injectedValueMapping[id]
    }
}
