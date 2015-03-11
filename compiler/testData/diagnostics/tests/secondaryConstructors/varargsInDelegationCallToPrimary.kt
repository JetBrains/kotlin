// !DIAGNOSTICS: -UNUSED_PARAMETER
fun <T> array(vararg x: T): Array<T> = null!!

open class B(vararg y: String) {
    constructor(x: Int): this(x.toString(), *array("1"), "2") {}
}

class A : B {
    constructor(x: String, y: String): super(x, *array("3"), y) {}
    constructor(x: String): super(x) {}
    constructor(): super() {}
}
