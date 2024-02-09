// NI_EXPECTED_FILE

typealias Array2D<T> = Array<Array<T>>

fun foo1(a: Array2D<out Number>) = a

fun bar1(a: Array2D<Int>) =
        foo1(<!ARGUMENT_TYPE_MISMATCH!>a<!>)


typealias TMap<T> = Map<T, T>

fun <T> foo2(m: TMap<T>) = m

fun bar2(m: TMap<*>) =
        <!CANNOT_INFER_PARAMETER_TYPE!>foo2<!>(<!ARGUMENT_TYPE_MISMATCH!>m<!>)
