typealias Array2D<T> = Array<Array<T>>

fun foo(a: Array2D<out Number>) = a

fun bar(a: Array2D<Int>) {
    foo(<!TYPE_MISMATCH!>a<!>)
}