// KT-9051: Allow smart cast for captured variables if they are not modified

fun foo(y: String?) {
    var x: String? = null
    if (x != null) {
        y?.let { x = it }
        x<!UNSAFE_CALL!>.<!>length // not-null or not-null
    }
}
