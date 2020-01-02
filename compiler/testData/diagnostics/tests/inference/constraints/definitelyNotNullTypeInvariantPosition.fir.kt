// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

class Inv<T>(val x: T?)

fun <K> create(y: K) = Inv(y)
fun <K> createPrivate(y: K) = Inv(y)

fun takeInvInt(i: Inv<Int>) {}

fun <S> test(i: Int, s: S) {
    val a = Inv(s)

    a

    val b = create(i)

    b

    val c = createPrivate(i)

    c

    takeInvInt(create(i))
}