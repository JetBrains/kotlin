// !LANGUAGE: +MultiPlatformProjects +UseGetterNameForPropertyAnnotationsMethodOnJvm
// !OPT_IN: kotlin.ExperimentalMultiplatform
// TARGET_BACKEND: JVM
// WITH_STDLIB

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: K2 incorrectly generates Anno.class, need to investigate after KT-57243 is fixed.

@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE") // TODO: support common sources in the test infrastructure

@OptionalExpectation
expect annotation class Anno(val s: String)

@Anno("Foo")
class Foo @Anno("<init>") constructor(@Anno("x") x: Int) {
    @Anno("bar")
    fun bar() {}

    @Anno("getX")
    var x = x
        @Anno("setX")
        set

    @Anno("Nested")
    interface Nested
}
