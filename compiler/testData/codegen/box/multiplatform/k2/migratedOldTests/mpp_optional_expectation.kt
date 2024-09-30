// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR, JS_IR, WASM, NATIVE
// WITH_STDLIB
// Stdlib is needed only for JVM_IR to resolve `kotlin.OptionalExpectation`

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
