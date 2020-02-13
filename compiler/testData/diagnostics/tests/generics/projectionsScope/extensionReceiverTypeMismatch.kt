// !WITH_NEW_INFERENCE
class A<T> {
    fun T.foo() {}
    fun Out<T>.bar() {}
}
class Out<out E>

fun test(x: A<out CharSequence>, y: Out<CharSequence>) {
    <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(x) {
        // TODO: this diagnostic could be replaced with TYPE_MISMATCH_DUE_TO_TYPE_PROJECTION
        "".<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
        <!OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>y<!>.<!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()

        <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(y) {
            <!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>bar<!>()
        }
    }
}
