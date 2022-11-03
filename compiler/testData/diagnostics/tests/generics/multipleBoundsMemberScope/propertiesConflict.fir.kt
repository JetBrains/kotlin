// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// !CHECK_TYPE

interface A {
    val foo: Any?
}

interface C: A {
    override val foo: String
}
interface B: A {
    override var foo: String?
}

fun <T> test(a: T) where T : B, T : C {
    a.foo = ""
    a.foo = null

    a.foo.checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() }
}
