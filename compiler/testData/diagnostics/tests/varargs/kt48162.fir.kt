// DIAGNOSTICS: -UNCHECKED_CAST

fun <T> Collection<T>.toArray(): Array<T> = this <!CAST_NEVER_SUCCEEDS!>as<!> Array<T>
fun Collection<String>.toArray2(): Array<String> = this <!CAST_NEVER_SUCCEEDS!>as<!> Array<String>
fun <T> toArray3(x: Collection<T>): Array<T> = x <!CAST_NEVER_SUCCEEDS!>as<!> Array<T>

class Foo<T> {
    operator fun plus(x: Foo<T>): Array<T> {
        return this + x
    }
}

fun use(arg: Array<String>, s: Collection<String>, x: Foo<String>) {
    arr(<!NON_VARARG_SPREAD!>*<!>arg)
    arr(<!NON_VARARG_SPREAD!>*<!>s.toArray())
    arr(<!NON_VARARG_SPREAD!>*<!>s.toArray2())
    arr(<!NON_VARARG_SPREAD!>*<!>toArray3(s))
    arr(<!NON_VARARG_SPREAD!>*<!>x + x)
    arr(<!NON_VARARG_SPREAD!>*<!>(x + x))
}

fun arr(x: Array<String>) {}
