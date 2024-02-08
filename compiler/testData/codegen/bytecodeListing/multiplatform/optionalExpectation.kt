// !LANGUAGE: +MultiPlatformProjects +UseGetterNameForPropertyAnnotationsMethodOnJvm
// !OPT_IN: kotlin.ExperimentalMultiplatform
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63984


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
