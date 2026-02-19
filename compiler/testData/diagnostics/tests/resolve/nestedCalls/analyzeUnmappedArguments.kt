// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
package c

fun test() {
    with (1) l@ {
        foo(1, <!NAMED_PARAMETER_NOT_FOUND!>zz<!> = { this@l } )
    }
}

fun foo(x: Int) = x

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, thisExpression */
