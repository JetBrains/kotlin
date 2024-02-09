// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS

interface I

interface Inv<P>
interface Out<out T>

class Bar<U : I>(val x: Inv<Out<U>>)

fun <T> materializeFoo(): Inv<T> = null as Inv<T>

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Bar<!>(<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeFoo<!>())
}