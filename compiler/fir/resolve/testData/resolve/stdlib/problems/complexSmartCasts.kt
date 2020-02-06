fun <T> runHigherOrder(f: () -> T): T = f()

val String.ext: Int get() = length

fun foo(a: Any?) {
    val s = a as? String
    val length = s?.ext ?: return
    runHigherOrder {
        s.isNotEmpty()
    }
}

fun bar(s: String?) {
    if (s?.isNotEmpty() == true) {
        s.length
    } else {
        s.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}

fun bar_2(s: String?) {
    if (s?.isNotEmpty() == false) {
        s.length
    } else {
        s.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}

fun bar_3(s: String?) {
    if (s?.isNotEmpty() != true) {
        s.<!INAPPLICABLE_CANDIDATE!>length<!>
    } else {
        s.length
    }
}

fun bar_4(s: String?) {
    if (s?.isNotEmpty() != false) {
        s.<!INAPPLICABLE_CANDIDATE!>length<!>
    } else {
        s.length
    }
}

fun baz(s: String?) {
    when {
        !s.isNullOrEmpty() -> s.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}

fun bazz(s: String?) {
    s?.let { use(it) }
}

class Wrapper(val s: String?) {
    fun withThis() {
        if (s != null) {
            use(this.s)
        }
        if (this.s != null) {
            use(s)
        }
    }
}

fun Any.withInvoke(f: String.() -> Unit) {
    if (this is String) {
        <!INAPPLICABLE_CANDIDATE!>f<!>()
    }
}

fun String.withInvoke(f: String.() -> Unit) {
    f()
}

fun <X> withBangBang(a: X) {
    if (a is String?) {
        <!INAPPLICABLE_CANDIDATE!>use<!>(a!!)
    }
}

fun use(s: String) {}

