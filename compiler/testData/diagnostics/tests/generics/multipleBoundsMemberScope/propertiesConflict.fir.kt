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

    a.foo.checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
}
