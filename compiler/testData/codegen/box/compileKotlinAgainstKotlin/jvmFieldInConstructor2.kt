// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

abstract class Foo<T : Number>(@JvmField val foo: T)

class Bar(foo: Int) : Foo<Int>(foo) {
    fun test(): Int = foo + 1
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    Bar(41).test()
    return "OK"
}
