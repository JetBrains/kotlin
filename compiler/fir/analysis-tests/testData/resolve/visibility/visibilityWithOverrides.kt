// ISSUE: KT-38656

// FILE: B.kt

class B : A() {
    override fun foo(s: String): String = ""

    fun testProtected(): String {
        return this foo "hello"
    }
}

// FILE: A.kt

abstract class A {
    protected abstract infix fun foo(s: String): String
}

// FILE: main.kt

fun test(b: B): String {
    return b <!INVISIBLE_REFERENCE!>foo<!> "hello" // should be an error
}
