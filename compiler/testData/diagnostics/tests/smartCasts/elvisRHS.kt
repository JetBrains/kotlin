// !DIAGNOSTICS: -UNUSED_VARIABLE

// KT-5335

fun foo(p1: String?, p2: String?) {
    if (p2 != null) {
        val v = p1 ?: <!DEBUG_INFO_SMARTCAST!>p2<!>
        val size = v.length
    }
}
