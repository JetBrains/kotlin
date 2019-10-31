// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

class Inv<T>(val x: T?)

fun <K> create(y: K) = Inv(y)
fun <K> createPrivate(y: K) = Inv(y)

fun takeInvInt(i: Inv<Int>) {}

fun <S> test(i: Int, s: S) {
    val a = Inv(s)

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<S>")!>a<!>

    val b = create(i)

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>b<!>

    val c = createPrivate(i)

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>c<!>

    takeInvInt(create(i))
}