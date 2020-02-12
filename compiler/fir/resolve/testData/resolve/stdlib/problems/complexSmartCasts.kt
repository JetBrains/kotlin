fun test_1(s: String?) {
    // contracts related
    when {
        !s.isNullOrEmpty() -> s.<!INAPPLICABLE_CANDIDATE!>length<!> // Should be OK
    }
}

fun test_2(s: String?) {
    s?.let {
        takeString(it) // Should be OK
        takeString(s) // Should be OK
    }
}

class Wrapper(val s: String?) {
    fun withThis() {
        if (s != null) {
            takeString(this.s) // Should be OK
        }
        if (this.s != null) {
            takeString(s) // Should be OK
        }
    }
}

fun Any.withInvoke(f: String.() -> Unit) {
    if (this is String) {
        <!INAPPLICABLE_CANDIDATE!>f<!>() // Should be OK
    }
}

fun String.withInvoke(f: String.() -> Unit) {
    f()
}

fun takeString(s: String) {}

