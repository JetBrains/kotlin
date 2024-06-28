// ISSUE: KT-64837
// MODULE: lib1
// FILE: Some.kt
class Some<T>

// MODULE: lib2(lib1)
// FILE: lib.kt
class Inv<T>

interface Base {
    val x: Inv<Some<*>>
}

// MODULE: main(lib2)
// FILE: main.kt
interface Derived : Base

fun box(): String = "OK"
