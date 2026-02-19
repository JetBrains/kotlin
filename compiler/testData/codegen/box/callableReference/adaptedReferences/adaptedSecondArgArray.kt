// WITH_STDLIB
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +RefinedVarargConversionRulesForCallableReferences
// ISSUE: KT-81913

fun foo(b: (Any, Array<String>) -> String): String {
    return b("O", arrayOf("K"))
}

fun of(vararg args: Any): String {
    return args[0].toString() + (args[1] as Array<String>)[0].toString()
}

fun box(): String {
    return foo(::of) // OK
}