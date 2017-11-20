// !WITH_NEW_INFERENCE
typealias Array2D<T> = Array<Array<T>>

fun foo1(a: Array2D<out Number>) = a

fun bar1(a: Array2D<Int>) =
        foo1(<!TYPE_MISMATCH(Array2D<out Number> /* = Array<Array<out Number>> */; Array2D<Int> /* = Array<Array<Int>> */)!>a<!>)


typealias TMap<T> = Map<T, T>

fun <T> foo2(m: TMap<T>) = m

fun bar2(m: TMap<*>) =
        <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>foo2<!>(m)
