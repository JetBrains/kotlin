/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer

import org.jetbrains.kotlin.cli.jvm.analyzer.scope.TypePredicate
import org.jetbrains.kotlin.cli.jvm.analyzer.scope.analyzer


val analyzers = listOf(
    functionDefinition(),
    ifThenElse(),
    functionName(),
    functionCall(),
    forLoop(),
    variableType(),
    whileLoop()
).map { it.first.title to it }.toMap()


fun functionDefinition() = analyzer("functionDefinition") {
    function {
        numberOfArguments = 2
        argument { type = TypePredicate("Int") }
        argument { type = TypePredicate("A") }
        returnType = TypePredicate("Int")

        info = { println("foo founded") }
    }
} to true


fun ifThenElse() = analyzer("ifThenElse") {
    function { body {
        ifCondition {
            thenBranch {
                variableDefinition { type = TypePredicate.Int }
            }
            elseBranch {
                variableDefinition { type = TypePredicate.Int }
            }
        }
    } }
} to true


fun functionName() = analyzer("functionName") {
    function {
        name = "foo"
    }
} to true


fun functionCall() = analyzer("functionCall") {
    val foo = function { name = "foo" }
    function { body {
        functionCall(foo)
    } }
} to true


fun forLoop() = analyzer("forLoop") {
    function { body {
        forLoop { body {
            variableDefinition { type = TypePredicate.Int }
        } }
    } }
} to true


fun variableType() = analyzer("variableType") {
    variableDefinition { type = TypePredicate.Int }
} to true


fun whileLoop() = analyzer("whileLoop") {
    function { body {
        variableDefinition { type = TypePredicate.Int }
    } }
} to true