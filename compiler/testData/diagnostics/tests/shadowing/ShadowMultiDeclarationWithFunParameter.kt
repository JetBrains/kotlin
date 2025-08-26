// FIR_IDENTICAL
// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: BACKEND
class A {
    operator fun component1() = 42
    operator fun component2() = 42
}

fun foo(a: A, c: Int) {
    val [a, b] = a
    val arr = Array(2) { A() }
    for ([c, d] in arr)  {

    }

    val e = a.toString() + b + c
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, destructuringDeclaration, forLoop, functionDeclaration,
integerLiteral, lambdaLiteral, localProperty, operator, propertyDeclaration */
