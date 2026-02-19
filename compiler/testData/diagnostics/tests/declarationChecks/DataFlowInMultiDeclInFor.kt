// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// KT-2667 Support multi-declarations in for-loops in control flow analysis
package d

class A {
    operator fun component1() = 1
    operator fun component2() = 2
    operator fun component3() = 3
}

fun foo(list: List<A>) {
    for (<!VAL_OR_VAR_ON_LOOP_PARAMETER!>var<!> [c1, c2, c3] in list) {
        <!VAL_REASSIGNMENT!>c1<!> = 1
        c3 + 1
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, forLoop, functionDeclaration, integerLiteral,
localProperty, operator, propertyDeclaration */
