/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import java.io.File

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) } ?: File("compiler/fir/checkers/gen").absoluteFile

    val expressionPackage = "org.jetbrains.kotlin.fir.analysis.checkers.expression"
    generateCheckersComponents(generationPath, expressionPackage, "FirExpressionChecker") {
        alias<FirStatement>("BasicExpressionChecker")
        alias<FirQualifiedAccessExpression>("QualifiedAccessChecker")
        alias<FirFunctionCall>("FunctionCallChecker")
        alias<FirVariableAssignment>("VariableAssignmentChecker")
    }

    val declarationPackage = "org.jetbrains.kotlin.fir.analysis.checkers.declaration"
    generateCheckersComponents(generationPath, declarationPackage, "FirDeclarationChecker") {
        alias<FirDeclaration>("BasicDeclarationChecker")
        alias<FirMemberDeclaration>("MemberDeclarationChecker")
        alias<FirRegularClass>("RegularClassChecker")
        alias<FirConstructor>("ConstructorChecker")
        alias<FirFile>("FileChecker")

        additional(
            fieldName = "controlFlowAnalyserCheckers",
            classFqn = "org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker"
        )

        additional(
            fieldName = "variableAssignmentCfaBasedCheckers",
            classFqn = "org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker"
        )
    }
}

/*
 * Stages:
 *   1. associate aliases with fir statements
 *   2. build inheritance hierarchy for all mentioned fir elements
 *   3. generate aliases
 *   4. generate abstract "DeclarationCheckers"
 *   5. generate "ComposedDeclarationCheckers" with OptIn annotation
 */
