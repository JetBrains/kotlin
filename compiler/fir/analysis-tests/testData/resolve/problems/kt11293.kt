// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-11293

// KT-11293: False warning "No cast needed" for lambda argument

fun foo(f: (Int) -> Double) = 2.0
fun foo(f: (Int) -> Int) = 1

fun main(args: Array<String>) {
    foo({i: Int -> 1.0} <!USELESS_CAST!>as (Int) -> Double<!>) // should NOT produce USELESS_CAST warning
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, functionalType, integerLiteral, lambdaLiteral */
