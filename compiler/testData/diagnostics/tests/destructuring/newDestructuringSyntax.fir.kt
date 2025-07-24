// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -NameBasedDestructuring, -DeprecateNameMismatchInShortDestructuringWithParentheses, -EnableNameBasedDestructuringShortForm
data class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) { (<!UNSUPPORTED_FEATURE!>val<!> first, <!UNSUPPORTED_FEATURE!>var<!> second,) = x }
    if (true) { <!UNSUPPORTED_FEATURE!>[<!>val first, var second,] = x }
    if (true) { val <!UNSUPPORTED_FEATURE!>[<!>first, second,] = x }
}

fun loop(x: List<Tuple>) {
    for ((<!UNSUPPORTED_FEATURE!>val<!> first, <!UNSUPPORTED_FEATURE!>val<!> second,) in x) {}
    for (<!UNSUPPORTED_FEATURE!>[<!>val first] in x) {}
    for (<!UNSUPPORTED_FEATURE!>[<!>first: String] in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (<!UNSUPPORTED_FEATURE!>val<!> first, <!UNSUPPORTED_FEATURE!>val<!> second,) -> }
    foo { <!UNSUPPORTED_FEATURE!>[<!>val first] -> }
    foo { <!UNSUPPORTED_FEATURE!>[<!>first: String] -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
