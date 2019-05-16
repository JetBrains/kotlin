// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

interface Inv<T1>
class InvImpl<T2> : Inv<T2>
open class OpenInv<T>

class A<T> {
    fun <F : OpenInv<T>> foo(x: F) = 1

}
class B<T> {
    fun <F : Inv<T>> foo(x: F) = 1

}

fun test(a: A<out CharSequence>, b: B<out CharSequence>) {
    a.<!OI;TYPE_INFERENCE_INCORPORATION_ERROR!>foo<!>(<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>OpenInv<!>())
    b.<!OI;TYPE_INFERENCE_INCORPORATION_ERROR!>foo<!>(<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>InvImpl<!>())
}