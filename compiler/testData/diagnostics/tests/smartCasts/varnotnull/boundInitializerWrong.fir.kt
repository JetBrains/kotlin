// !WITH_NEW_INFERENCE
// KT-15792 and related

fun foo() {
    var x: String? = ""
    val y = x
    x = null
    if (y != null) {
        x.hashCode()
    }
}

fun foo2() {
    var x: String? = ""
    val y = x
    if (y != null) {
        x.hashCode()
    }
}

fun bar(s: String?) {
    var ss = s
    val hashCode = ss?.hashCode()
    ss = null
    if (hashCode != null) {
        ss.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
    }
}

fun bar2(s: String?) {
    var ss = s
    val hashCode = ss?.hashCode()
    if (hashCode != null) {
        ss.hashCode()
    }
}

class Some(var s: String?)

fun baz(arg: Some?) {
    val ss = arg?.s
    if (ss != null) {
        arg.hashCode()
        arg.s.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
    }
}
