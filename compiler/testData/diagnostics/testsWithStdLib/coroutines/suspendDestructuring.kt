// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: BACKEND
class A {
    suspend operator fun component1(): String = "K"
}

fun foo(c: suspend (A) -> Unit) {}

fun bar() {
    foo {
        [x] ->
        x.length
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty, operator,
propertyDeclaration, stringLiteral, suspend */
