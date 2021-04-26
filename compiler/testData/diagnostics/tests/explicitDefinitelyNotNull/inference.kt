// !LANGUAGE: +DefinitelyNotNullTypeParameters

fun <T> toDefNotNull(s: T): T!! = s!!

fun <K> removeQuestionMark(x: K?): K = x!!

fun Any.foo() {}

fun <E> expectNN(e: E!!) {}

fun <F> main(x: F, y: F, z: F, w: F, m: F) {
    val y1 = toDefNotNull(x) // K instead of K!!
    val y2: F!! = toDefNotNull(x) // K instead of K!!
    val x1 = removeQuestionMark(x) // T or T!!
    val x2: F!! = removeQuestionMark(x) // T or T!!

    val z1 = x!!
    val z2: F!! = y!!
    val w1 = if (z != null) <!DEBUG_INFO_SMARTCAST!>z<!> else return
    val w2: F!! = if (w != null) <!DEBUG_INFO_SMARTCAST!>w<!> else return

    y1.foo()
    y2.foo()
    x1.foo()
    x2.foo()
    z1.foo()
    z2.foo()
    w1.foo()
    w2.foo()

    expectNN(<!TYPE_MISMATCH!>m<!>)
    expectNN(m!!)
}
