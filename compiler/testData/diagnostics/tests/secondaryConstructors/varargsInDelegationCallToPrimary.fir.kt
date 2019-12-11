// !DIAGNOSTICS: -UNUSED_PARAMETER
fun <T> array(vararg x: T): Array<T> = null!!

open class B(vararg y: String) {
    constructor(x: Int): <!INAPPLICABLE_CANDIDATE!>this<!>(x.toString(), *array("1"), "2")
}

class A : B {
    constructor(x: String, y: String): <!INAPPLICABLE_CANDIDATE!>super<!>(x, *array("3"), y)
    constructor(x: String): <!INAPPLICABLE_CANDIDATE!>super<!>(x)
    constructor(): super()
}
