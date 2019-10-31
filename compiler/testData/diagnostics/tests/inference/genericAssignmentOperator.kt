class R<T>

fun <T> f(): R<T> = R<T>()

operator fun Int.plusAssign(<!UNUSED_PARAMETER!>y<!>: R<Int>) {}

fun box() {
    1 += f()
}