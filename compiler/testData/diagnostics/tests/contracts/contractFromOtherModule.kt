// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContractSyntaxV2
// WITH_STDLIB

// MODULE: lib
package lib

import kotlin.contracts.*

fun requireIsTrue(value: Boolean) contract [
    returns() implies value
] {
    if (!value) throw IllegalArgumentException()
}

// MODULE: main(lib)
package main

import lib.requireIsTrue

fun test(s: Any) {
    requireIsTrue(s is String)
    s.length
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, functionDeclaration, ifExpression, isExpression, smartcast */
