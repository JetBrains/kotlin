// !LANGUAGE: +NewInference
// !WITH_NEW_INFERENCE

interface B<T : S?, S : Any> {
    val t: T
}

class C(override val t: Any?) : B<Any?, Any>

fun f(b: B<*, Any>) {
    val y = <!OI;TYPE_MISMATCH!>b<!>.<!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>t<!>
    if (<!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>y<!> is String<!OI;USELESS_NULLABLE_CHECK!>?<!>) {
        <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, OI;DEBUG_INFO_SMARTCAST!>y<!>.<!NI;DEBUG_INFO_MISSING_UNRESOLVED!>length<!>
    }
}

fun main() {
    f(C("hello"))
    f(C(null))
}