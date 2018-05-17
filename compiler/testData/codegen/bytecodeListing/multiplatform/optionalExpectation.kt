// !LANGUAGE: +MultiPlatformProjects
// !USE_EXPERIMENTAL: kotlin.ExperimentalMultiplatform
// TARGET_BACKEND: JVM
// WITH_RUNTIME

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
