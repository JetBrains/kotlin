// WITH_RUNTIME

//FILE: a/a.kt
package foo

class A {
    val a by lazy {
        val a = 5
        val b = 2
        ""
    }
}

//FILE: b/a.kt
package bar

class B {
    val a by lazy {
        val b = 0
    }
}