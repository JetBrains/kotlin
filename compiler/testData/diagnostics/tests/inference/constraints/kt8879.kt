// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Inv<I>
interface Inv2<I>

fun <T: Inv2<T>> foo(klass: Inv<T>): String? = null

fun <X> bar(): Inv<X> = null!!

fun test() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>())
}