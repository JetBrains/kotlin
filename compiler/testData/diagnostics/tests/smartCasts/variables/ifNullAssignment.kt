// !WITH_NEW_INFERENCE
// See KT-13468, KT-13765

fun basic(): String {
    var current: String? = null
    current = if (current == null) "bar" else current
    return <!DEBUG_INFO_SMARTCAST!>current<!>
}

fun foo(flag: Boolean) {
    var x: String? = null

    if (x == null) {
        x = if (flag) "34" else "12"
    }

    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
}

fun bar(flag: Boolean) {
    var x: String? = null

    if (x == null) {
        x = when {
            flag -> "34"
            else -> "12"
        }
    }

    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
}

fun baz(flag: Boolean) {
    var x: String? = null

    if (x == null) {
        x = if (flag) {
            "34"
        } else {
            "12"
        }
    }

    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
}

fun gav(flag: Boolean, arg: String?) {
    var x: String? = null

    if (x == null) {
        x = arg ?: if (flag) {
            "34"
        } else {
            "12"
        }
    }

    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
}

fun gau(flag: Boolean, arg: String?) {
    var x: String? = null

    if (x == null) {
        x = if (flag) {
            arg ?: "34"
        } else {
            arg ?: "12"
        }
    }

    <!NI;DEBUG_INFO_SMARTCAST!>x<!><!OI;UNSAFE_CALL!>.<!>hashCode()
}