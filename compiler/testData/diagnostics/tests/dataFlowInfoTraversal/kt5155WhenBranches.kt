//KT-5155 Auto-casts do not work with when

fun foo(s: String?) {
    when {
        s == null -> <!UNUSED_EXPRESSION!>1<!>
        <!DEBUG_INFO_AUTOCAST!>s<!>.foo() -> <!UNUSED_EXPRESSION!>2<!>
        else -> <!UNUSED_EXPRESSION!>3<!>
    }
}

fun String.foo() = true