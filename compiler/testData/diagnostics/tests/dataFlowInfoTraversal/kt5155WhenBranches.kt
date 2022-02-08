//KT-5155 Auto-casts do not work with when

fun foo(s: String?) {
    when {
        s == null -> 1
        <!DEBUG_INFO_SMARTCAST!>s<!>.foo() -> 2
        else -> 3
    }
}

fun String.foo() = true
