// FILE: A.kt

interface A : B

fun box(): String = object : A {}.foo()

// FILE: B.kt

interface Base {
    fun foo(s: String = "OK"): String = s
}

interface B : Base