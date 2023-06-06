// !LANGUAGE: +DefinitelyNonNullableTypes

fun <T> toDefNotNull(s: T): T & Any = s!!

fun <K> removeQuestionMark(x: K?): K = x!!

fun Any.foo() {}

fun <E> expectNN(e: E & Any) {}

fun <F> main(x: F, y: F, z: F, w: F, m: F) {
    val y1 = toDefNotNull(x) // K instead of K & Any
    val y2: F & Any = toDefNotNull(x) // K instead of K & Any
    val x1 = removeQuestionMark(x) // T or T & Any
    val x2: F & Any = removeQuestionMark(x) // T or T & Any

    val z1 = x!!
    val z2: F & Any = y!!
    val w1 = if (z != null) z else return
    val w2: F & Any = if (w != null) w else return

    y1.foo()
    y2.foo()
    x1.foo()
    x2.foo()
    z1.foo()
    z2.foo()
    w1.foo()
    w2.foo()

    expectNN(<!ARGUMENT_TYPE_MISMATCH!>m<!>)
    expectNN(m!!)
}
