// !LANGUAGE: -SafeCallBoundSmartCasts

fun foo(arg: Int?) {
    val x = arg
    if (x != null) {
        arg<!UNSAFE_CALL!>.<!>hashCode()
    }
    val y: Any? = arg
    if (y != null) {
        arg<!UNSAFE_CALL!>.<!>hashCode()
    }
    val yy: Any?
    yy = arg
    if (yy != null) {
        arg<!UNSAFE_CALL!>.<!>hashCode()
    }
    var z = arg
    z = z?.let { 42 }
    if (z != null) {
        arg<!UNSAFE_CALL!>.<!>hashCode()
    }
}

fun kt6840_1(s: String?) {
    val hash = s?.hashCode()
    if (hash != null) {
        s<!UNSAFE_CALL!>.<!>length
    }
}

fun kt6840_2(s: String?) {
    if (s?.hashCode() != null) {
        s<!UNSAFE_CALL!>.<!>length
    }
}

fun kt1635(s: String?) {
    s?.hashCode()!!
    s<!UNSAFE_CALL!>.<!>hashCode()
}

fun kt2127() {
    val s: String? = ""
    if (s?.length != null) {
        s<!UNSAFE_CALL!>.<!>hashCode()
    }
}

fun kt3356(s: String?): Int {
    if (s?.length != 1) return 0
    return s<!UNSAFE_CALL!>.<!>length
}
