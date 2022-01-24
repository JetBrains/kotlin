/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JS_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JVM_DIAGNOSTICS_LIST
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
        alias<FirQualifiedAccess>("QualifiedAccessChecker")
        alias<FirQualifiedAccessExpression>("QualifiedAccessExpressionChecker")
        alias<FirCall>("CallChecker")
        alias<FirFunctionCall>("FunctionCallChecker")
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
        alias<FirConstExpression<*>>("ConstExpressionChecker")
        alias<FirCallableReferenceAccess>("CallableReferenceAccessChecker")
        alias<FirThisReceiverExpression>("ThisReceiverExpressionChecker")
        alias<FirWhileLoop>("WhileLoopChecker")
        alias<FirDoWhileLoop>("DoWhileLoopChecker")
        alias<FirArrayOfCall>("ArrayOfCallChecker")
        alias<FirClassReferenceExpression>("ClassReferenceExpressionChecker")
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

    generateDiagnostics(generationPath, "$basePackage.diagnostics", DIAGNOSTICS_LIST)
    generateDiagnostics(jvmGenerationPath, "$basePackage.diagnostics.jvm", JVM_DIAGNOSTICS_LIST)
    generateDiagnostics(jsGenerationPath, "$basePackage.diagnostics.js", JS_DIAGNOSTICS_LIST)
}

/*
 * Stages:
 *   1. associate aliases with fir statements
 *   2. build inheritance hierarchy for all mentioned fir elements
 *   3. generate aliases
 *   4. generate abstract "DeclarationCheckers"
 *   5. generate "ComposedDeclarationCheckers" with OptIn annotation
 */
