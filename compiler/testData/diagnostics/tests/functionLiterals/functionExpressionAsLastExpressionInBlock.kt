// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION

import java.util.HashSet

fun test123() {
    val g: (Int) -> Unit = if (true) {
        val set = HashSet<Int>()
        fun (i: Int) {
            set.add(i)
        }
    }
    else {
        { it -> it }
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, flexibleType, functionDeclaration, functionalType, ifExpression, javaFunction,
lambdaLiteral, localProperty, propertyDeclaration */
