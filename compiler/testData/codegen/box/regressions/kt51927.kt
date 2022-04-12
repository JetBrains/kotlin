// TARGET_BACKEND: NATIVE
// MODULE: inner
// FILE: Foo.kt
class Foo(parameter: String)

// MODULE: outer(inner)
// FILE: FooFactory.kt
fun Foo(): Foo = Foo("")

// MODULE: main(inner, outer)
// FILE: box.kt
fun box(): String {
    Foo()
    return "OK"
}