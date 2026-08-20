// LANGUAGE: +MultiPlatformProjects
// IGNORE_HMPP: ANY
// ISSUE: KT-88721

// MODULE: main-common
open class Foo {
    internal fun foo(): String = "OK"
}

// MODULE: main-platform()()(main-common)

// MODULE: test-common()(main-common)()
class Bar : Foo()

// MODULE: test-platform()(main-platform)(test-common)
fun box(): String {
    return Bar().foo()
}
