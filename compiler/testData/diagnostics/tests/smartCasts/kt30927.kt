// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_NEW_INFERENCE
// !CHECK_TYPE

fun case_0() {
    val z: Any? = 10
    val y = z.run {
        this as Int
        <!DEBUG_INFO_SMARTCAST{NI}!>this<!> // error in NI: required Int, found Any?; just inferred to Any? in OI
    }
    y checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>_<!><Any?>() }
    y checkType { <!TYPE_MISMATCH{OI}!>_<!><Int>() }
}

fun case_1(z: Any?) {
    val y = z.run {
        when (this) {
            is String -> return@run <!DEBUG_INFO_SMARTCAST{NI}!>this<!> // type mismatch in the new inference (required String, found Any?)
            is Float -> ""
            else -> return@run ""
        }
    }
    y checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>_<!><Any?>() }
    y checkType { <!TYPE_MISMATCH{OI}!>_<!><kotlin.String>() }
    // y is inferred to Any?
}

fun case_2(z: Any?) {
    val y = z.run {
        when (this) {
            is String -> <!DEBUG_INFO_SMARTCAST!>this<!>
            is Float -> ""
            else -> return@run ""
        }
    }
    y checkType { _<kotlin.String>() }
    // y is inferred to String
}

fun case_3(z: Any?) {
    val y = z.let {
        when (it) {
            is String -> return@let <!DEBUG_INFO_SMARTCAST{NI}!>it<!>
            is Float -> ""
            else -> return@let ""
        }
    }
    y checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>_<!><Any?>() }
    y checkType { <!TYPE_MISMATCH{OI}!>_<!><kotlin.String>() }
    // y is inferred to String
}
