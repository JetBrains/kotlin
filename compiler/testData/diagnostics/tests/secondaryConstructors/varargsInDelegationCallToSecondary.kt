// !DIAGNOSTICS: -UNUSED_PARAMETER
fun <T> array(vararg x: T): Array<T> = null!!

open class B(x: Int) {
    constructor(vararg y: String): this(y[0].length()) {}
}

class A : B {
    constructor(x: String, y: String): super(x, *array("q"), y) {}
    constructor(x: String): super(x) {}
    constructor(): super() {}
}

val b1 = B()
val b2 = B("1", "2", "3")
val b3 = B("1", *array("2", "3"), "4")
val b4 = B(1)
