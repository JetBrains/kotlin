// MODULE: lib
// FILE: lib.kt

enum class Foo {
    Z;

    open fun bar() = "OK"
}

// MODULE: main(lib)
// FILE: main.kt

fun box() = Foo.Z.bar()