// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_NEW_INFERENCE
// !CHECK_TYPE

fun case_0() {
    val z: Any? = 10
    val y = z.run {
        this as Int
        <!NI;DEBUG_INFO_SMARTCAST!>this<!> // error in NI: required Int, found Any?; just inferred to Any? in OI
    }
    y checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any?>() }
    y checkType { <!OI;TYPE_MISMATCH!>_<!><Int>() }
}

fun case_1(z: Any?) {
    val y = z.run {
        when (this) {
            is String -> return@run <!NI;DEBUG_INFO_SMARTCAST!>this<!> // type mismatch in the new inference (required String, found Any?)
            is Float -> ""
            else -> return@run ""
        }
    }
    y checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any?>() }
    y checkType { <!OI;TYPE_MISMATCH!>_<!><kotlin.String>() }
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
            is String -> return@let <!NI;DEBUG_INFO_SMARTCAST!>it<!>
            is Float -> ""
            else -> return@let ""
        }
    }
    y checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any?>() }
    y checkType { <!OI;TYPE_MISMATCH!>_<!><kotlin.String>() }
    // y is inferred to String
}