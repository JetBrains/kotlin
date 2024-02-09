// !DIAGNOSTICS: -UNUSED_PARAMETER

class Out<out T>
class In<in T>
class Inv<T>

fun <R> choose1(c: Out<Out<R>>) {}
fun <R> choose2(c: In<In<R>>) {}
fun <R> choose3(c: Inv<Inv<R>>) {}

fun f(o: Out<Out<*>>, i: In<In<*>>, inv: Inv<Inv<*>>) {
    choose1(o)
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>choose2<!>(i)
    <!CANNOT_INFER_PARAMETER_TYPE!>choose3<!>(<!ARGUMENT_TYPE_MISMATCH!>inv<!>)
}
