package typeInferenceExpectedTypeMismatch

import java.util.*

fun test() {
    val <!UNUSED_VARIABLE!>s<!> : Set<Int> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>newList<!>()
}

fun newList<S>() : ArrayList<S> {
    return ArrayList<S>()
}
