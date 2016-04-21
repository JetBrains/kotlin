// MODULE: m1
// FILE: 1.kt

package a

class b {
    class c
}

// MODULE: m2
// FILE: 2.kt

package a.b

class c {
    fun foo() {}
}

// MODULE: m3(m1, m2)
// FILE: test.kt

package test

fun test() = a.b.c::foo
