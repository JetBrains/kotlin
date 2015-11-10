// KT-9051: Allow smart cast for captured variables if they are not modified

fun foo(y: String?) {
    var x: String? = null
    if (x != null) {
        y?.let { x = it }
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length // not-null or not-null
    }
}
