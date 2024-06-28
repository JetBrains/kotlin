// FIR_IDENTICAL
interface A {
    fun foo()
}

interface B : A {
    fun bar()
}

interface C : A {
    fun baz()
}

fun test(param: B) {
    if (param is C) {
        param.foo()
        param.bar()
        param.baz()
    }
}
