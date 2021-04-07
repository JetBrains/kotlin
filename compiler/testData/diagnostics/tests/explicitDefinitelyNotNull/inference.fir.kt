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
    val w1 = if (z != null) z else return
    val w2: F!! = if (w != null) w else return

    y1.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    y2.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    x1.foo()
    x2.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    z1.foo()
    z2.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    w1.foo()
    w2.<!INAPPLICABLE_CANDIDATE!>foo<!>()

    expectNN(m)
    expectNN(m!!)
}
