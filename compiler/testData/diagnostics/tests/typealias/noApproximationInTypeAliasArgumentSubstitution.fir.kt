// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

typealias Array2D<T> = Array<Array<T>>

fun foo1(a: Array2D<out Number>) = a

fun bar1(a: Array2D<Int>) =
        <!INAPPLICABLE_CANDIDATE!>foo1<!>(a)


typealias TMap<T> = Map<T, T>

fun <T> foo2(m: TMap<T>) = m

fun bar2(m: TMap<*>) =
        <!INAPPLICABLE_CANDIDATE!>foo2<!>(m)
