// CHECK_TYPE

interface A {
    fun foo(): CharSequence?
}

interface B : A {
    override fun foo(): String
}

fun test(a: A) {
    if (a is B) {
        a.foo()
        a.foo().checkType { _<String>() }
    }
}
