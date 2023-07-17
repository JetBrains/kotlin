// IGNORE_BACKEND: JS
// IGNORE_BACKEND_K2: ANY

// Test for KT-36188 bug compatibility between non-IR and IR backends

interface A {
    fun foo(a: String = "OK"): String
}

interface A2 : A

interface B {
    fun foo(a: String = "Fail"): String
}

interface C : A2, B

class Impl : C {
    override fun foo(a: String) = a
}

fun box(): String = Impl().foo()
