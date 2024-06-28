// DIAGNOSTICS: -UNCHECKED_CAST

fun <T> Collection<T>.toArray(): Array<T> = this as Array<T>
fun Collection<String>.toArray2(): Array<String> = this as Array<String>
fun <T> toArray3(x: Collection<T>): Array<T> = x as Array<T>

class Foo<T> {
    operator fun plus(x: Foo<T>): Array<T> {
        return this + x
    }
}

fun use(arg: Array<String>, s: Collection<String>, x: Foo<String>) {
    arr(<!NON_VARARG_SPREAD_ERROR!>*<!>arg)
    arr(<!NON_VARARG_SPREAD_ERROR!>*<!>s.toArray())
    arr(<!NON_VARARG_SPREAD_ERROR!>*<!>s.toArray2())
    arr(<!NON_VARARG_SPREAD_ERROR!>*<!>toArray3(s))
    arr(<!NON_VARARG_SPREAD_ERROR!>*<!>x + x)
    arr(<!NON_VARARG_SPREAD_ERROR!>*<!>(x + x))
}

fun arr(x: Array<String>) {}
