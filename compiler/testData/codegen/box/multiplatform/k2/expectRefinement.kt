// LANGUAGE: +MultiPlatformProjects +ExpectRefinement
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect class Foo {
    fun foo(): String
}

fun common(foo: Foo): String {
    return foo.foo()
}

// MODULE: intermediate()()(common)
// FILE: intermediate.kt

@kotlin.experimental.ExpectRefinement
expect class Foo {
    fun foo(): String
    fun bar(): String
}

fun intermediate(foo: Foo): String {
    return foo.bar()
}

// MODULE: platform()()(intermediate)
// FILE: platform.kt

actual class Foo {
    actual fun foo(): String = "O"
    actual fun bar(): String = "K"
}

fun box(): String = common(Foo()) + intermediate(Foo())
