class R<T>

fun <T> f(): R<T> = R<T>()

operator fun Int.plusAssign(y: R<Int>) {}

fun box() {
    <!INAPPLICABLE_CANDIDATE!>1 += f()<!>
}