// !LANGUAGE: +NewInference
// !WITH_NEW_INFERENCE

interface B<T : S?, S : Any> {
    val t: T
}

class C(override val t: Any?) : B<Any?, Any>

fun f(b: B<*, Any>) {
    val y = <!TYPE_MISMATCH{OI}!>b<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>t<!>
    if (<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{NI}!>y<!> is String<!USELESS_NULLABLE_CHECK{OI}!>?<!>) {
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{NI}, DEBUG_INFO_SMARTCAST{OI}!>y<!>.<!DEBUG_INFO_MISSING_UNRESOLVED{NI}!>length<!>
    }
}

fun main() {
    f(C("hello"))
    f(C(null))
}
