// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -NameBasedDestructuring, -DeprecateNameMismatchInShortDestructuringWithParentheses, -EnableNameBasedDestructuringShortForm
data class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) { <!UNSUPPORTED!>(val first, var second,) = x<!> }
    if (true) { <!UNSUPPORTED!>[val first, var second,] = x<!> }
    if (true) { <!UNSUPPORTED!>val [first, second,] = x<!> }
}

fun loop(x: List<Tuple>) {
    for (<!UNSUPPORTED!>(val first, val second,)<!> in x) {}
    for (<!UNSUPPORTED!>[val first]<!> in x) {}
    for (<!UNSUPPORTED!>[first: String]<!> in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { <!UNSUPPORTED!>(val first, val second,)<!> -> }
    foo { <!UNSUPPORTED!>[val first]<!> -> }
    foo { <!UNSUPPORTED!>[first: String]<!> -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, forLoop, functionDeclaration, functionalType,
ifExpression, lambdaLiteral, localFunction, localProperty, primaryConstructor, propertyDeclaration */
