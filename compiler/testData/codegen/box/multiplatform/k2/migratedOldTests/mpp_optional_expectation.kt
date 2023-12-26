// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: WASM
// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-62931

// Stdlib is needed only for JVM_IR to resolve `kotlin.OptionalExpectation
// WITH_STDLIB
// LANGUAGE: +MultiPlatformProjects

@file:Suppress("OPT_IN_USAGE_ERROR", "OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")

@OptionalExpectation
expect annotation class Optional()

@Optional
fun foo() = "42"

fun bar() = "43"

fun box(): String {
    val foo = foo()
    if (foo != "42")
        return "foo is wrongly $foo"

    val bar = bar()
    if (bar != "43")
        return "bar is wrongly $bar"

    return "OK"
}
