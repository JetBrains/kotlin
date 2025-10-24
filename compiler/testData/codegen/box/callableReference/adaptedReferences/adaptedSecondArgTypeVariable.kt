// WITH_STDLIB
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +RefinedVarargConversionRulesForCallableReferences
// ISSUE: KT-81841

fun <T> foo(b: (Any, T) -> String): String {
    return b("O", "K" as T)
}

fun of(vararg args: Any): String {
    return args[0].toString() + args[1].toString()
}

fun box(): String {
    return foo(::of)
}