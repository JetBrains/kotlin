// LANGUAGE: -NameBasedDestructuring -DeprecateNameMismatchInShortDestructuringWithParentheses -EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package test

fun a(s: String) { // <- ERROR
    val (x, y) = Pair("", s)
    println(x + y)
}

fun b(s: String) {
    val x = Pair("", s)
    println(x)
}

//from library
data class Pair<A, B>(val a: A, val b: B)

fun println(a: Any?) = a

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, data, destructuringDeclaration, functionDeclaration,
localProperty, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeParameter */
