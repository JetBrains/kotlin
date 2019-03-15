// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JVM_IR

interface A {
    private fun foo() = "OK"

    public fun bar() = foo()
}

class B : A {
    private fun foo() = "fail"
}

fun box() = B().bar()