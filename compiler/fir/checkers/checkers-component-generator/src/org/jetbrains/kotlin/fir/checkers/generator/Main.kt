/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.fir.builder.SYNTAX_DIAGNOSTIC_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JS_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JVM_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.NATIVE_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.WASM_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.WEB_COMMON_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.ErrorListDiagnosticListRenderer
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.generateDiagnostics
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.FirTypeRef
import java.io.File

fun main(args: Array<String>) {
    val arguments = args.toList()
    val generationPath = arguments.firstOrNull()?.let { File(it) } ?: File("compiler/fir/checkers/gen").absoluteFile

    val basePackage = "org.jetbrains.kotlin.fir.analysis"

    val typePackage = "$basePackage.checkers.type"
    generateCheckersComponents(generationPath, typePackage, "FirTypeChecker") {
        alias<FirTypeRef>("TypeRefChecker")
    }

    val expressionPackage = "$basePackage.checkers.expression"
    generateCheckersComponents(generationPath, expressionPackage, "FirExpressionChecker") {
        alias<FirStatement>("BasicExpressionChecker")
        alias<FirQualifiedAccessExpression>("QualifiedAccessExpressionChecker")
        alias<FirCall>("CallChecker")
        alias<FirFunctionCall>("FunctionCallChecker")
        alias<FirPropertyAccessExpression>("PropertyAccessExpressionChecker")
        alias<FirIntegerLiteralOperatorCall>("IntegerLiteralOperatorCallChecker")
        alias<FirVariableAssignment>("VariableAssignmentChecker")
        alias<FirTryExpression>("TryExpressionChecker")
        alias<FirWhenExpression>("WhenExpressionChecker")
        alias<FirLoop>("LoopExpressionChecker")
        alias<FirLoopJump>("LoopJumpChecker")
        alias<FirBinaryLogicExpression>("LogicExpressionChecker")
        alias<FirReturnExpression>("ReturnExpressionChecker")
        alias<FirBlock>("BlockChecker")
        alias<FirAnnotation>("AnnotationChecker")
        alias<FirAnnotationCall>("AnnotationCallChecker")
        alias<FirCheckNotNullCall>("CheckNotNullCallChecker")
        alias<FirElvisExpression>("ElvisExpressionChecker")
        alias<FirGetClassCall>("GetClassCallChecker")
        alias<FirSafeCallExpression>("SafeCallExpressionChecker")
        alias<FirEqualityOperatorCall>("EqualityOperatorCallChecker")
        alias<FirStringConcatenationCall>("StringConcatenationCallChecker")
        alias<FirTypeOperatorCall>("TypeOperatorCallChecker")
        alias<FirResolvedQualifier>("ResolvedQualifierChecker")
        alias<FirLiteralExpression<*>>("LiteralExpressionChecker")
        alias<FirCallableReferenceAccess>("CallableReferenceAccessChecker")
        alias<FirThisReceiverExpression>("ThisReceiverExpressionChecker")
        alias<FirWhileLoop>("WhileLoopChecker")
        alias<FirThrowExpression>("ThrowExpressionChecker")
        alias<FirDoWhileLoop>("DoWhileLoopChecker")
        alias<FirArrayLiteral>("ArrayLiteralChecker")
        alias<FirClassReferenceExpression>("ClassReferenceExpressionChecker")
        alias<FirInaccessibleReceiverExpression>("InaccessibleReceiverChecker")
    }

    val declarationPackage = "$basePackage.checkers.declaration"
    generateCheckersComponents(generationPath, declarationPackage, "FirDeclarationChecker") {
        alias<FirDeclaration>("BasicDeclarationChecker")
        alias<FirCallableDeclaration>("CallableDeclarationChecker")
        alias<FirFunction>("FunctionChecker")
        alias<FirSimpleFunction>("SimpleFunctionChecker")
        alias<FirProperty>("PropertyChecker")
        alias<FirClassLikeDeclaration>("ClassLikeChecker")
        alias<FirClass>("ClassChecker")
        alias<FirRegularClass>("RegularClassChecker")
        alias<FirConstructor>("ConstructorChecker")
        alias<FirFile>("FileChecker")
        alias<FirScript>("ScriptChecker")
        alias<FirTypeParameter>("FirTypeParameterChecker")
        alias<FirTypeAlias>("TypeAliasChecker")
        alias<FirAnonymousFunction>("AnonymousFunctionChecker")
        alias<FirPropertyAccessor>("PropertyAccessorChecker")
        alias<FirBackingField>("BackingFieldChecker")
        alias<FirValueParameter>("ValueParameterChecker")
        alias<FirEnumEntry>("EnumEntryChecker")
        alias<FirAnonymousObject>("AnonymousObjectChecker")
        alias<FirAnonymousInitializer>("AnonymousInitializerChecker")

        additional(
            fieldName = "controlFlowAnalyserCheckers",
            classFqn = "$basePackage.checkers.cfa.FirControlFlowChecker"
        )

        additional(
            fieldName = "variableAssignmentCfaBasedCheckers",
            classFqn = "$basePackage.cfa.AbstractFirPropertyInitializationChecker"
        )
    }

    val jvmGenerationPath = File(arguments.getOrElse(1) { "compiler/fir/checkers/checkers.jvm/gen" })
    val jsGenerationPath = File(arguments.getOrElse(2) { "compiler/fir/checkers/checkers.js/gen" })
    val nativeGenerationPath = File(arguments.getOrElse(3) { "compiler/fir/checkers/checkers.native/gen" })
    val wasmGenerationPath = File(arguments.getOrElse(4) { "compiler/fir/checkers/checkers.wasm/gen" })
    val webCommonGenerationPath = File(arguments.getOrElse(5) { "compiler/fir/checkers/checkers.web.common/gen" })
    val rawFirGenerationPath = File("compiler/fir/raw-fir/raw-fir.common/gen")

    val packageName = "$basePackage.diagnostics"

    generateDiagnostics(generationPath, packageName, DIAGNOSTICS_LIST, starImportsToAdd = setOf(ErrorListDiagnosticListRenderer.BASE_PACKAGE, ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE))
    generateDiagnostics(jvmGenerationPath, "$packageName.jvm", JVM_DIAGNOSTICS_LIST, starImportsToAdd = setOf(ErrorListDiagnosticListRenderer.BASE_PACKAGE, ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE))
    generateDiagnostics(jsGenerationPath, "$packageName.js", JS_DIAGNOSTICS_LIST, starImportsToAdd = setOf(ErrorListDiagnosticListRenderer.BASE_PACKAGE, ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE))
    generateDiagnostics(nativeGenerationPath, "$packageName.native", NATIVE_DIAGNOSTICS_LIST, starImportsToAdd = setOf(ErrorListDiagnosticListRenderer.BASE_PACKAGE, ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE))
    generateDiagnostics(wasmGenerationPath, "$packageName.wasm", WASM_DIAGNOSTICS_LIST, starImportsToAdd = setOf(ErrorListDiagnosticListRenderer.BASE_PACKAGE, ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE))
    generateDiagnostics(webCommonGenerationPath, "$packageName.web.common", WEB_COMMON_DIAGNOSTICS_LIST, starImportsToAdd = setOf(ErrorListDiagnosticListRenderer.BASE_PACKAGE, ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE))
    generateDiagnostics(rawFirGenerationPath, "org.jetbrains.kotlin.fir.builder", SYNTAX_DIAGNOSTIC_LIST, starImportsToAdd = setOf(ErrorListDiagnosticListRenderer.DIAGNOSTICS_PACKAGE))

    generateNonSuppressibleErrorNamesFile(generationPath, packageName)
}

/*
 * Stages:
 *   1. associate aliases with fir statements
 *   2. build inheritance hierarchy for all mentioned fir elements
 *   3. generate aliases
 *   4. generate abstract "DeclarationCheckers"
 *   5. generate "ComposedDeclarationCheckers" with OptIn annotation
 */
