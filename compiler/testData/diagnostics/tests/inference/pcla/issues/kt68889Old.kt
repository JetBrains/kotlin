// LANGUAGE: -NameBasedDestructuring -DeprecateNameMismatchInShortDestructuringWithParentheses -EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-68889

fun main(s: String?) {
    val a = buildList {
        val (_, matchResult) = s?.let { 1 to it } ?: return@buildList
        add(matchResult)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.String>")!>a<!>
}

/* GENERATED_FIR_TAGS: destructuringDeclaration, elvisExpression, functionDeclaration, integerLiteral, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, safeCall, unnamedLocalVariable */
