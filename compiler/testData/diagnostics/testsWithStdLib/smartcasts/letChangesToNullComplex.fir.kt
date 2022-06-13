// KT-9051: Allow smart cast for captured variables if they are not modified

fun bar(z: String?) = z

fun foo(y: String?) {
    var x: String? = ""
    if (x != null) {
        bar(y?.let { x = null; it })<!UNSAFE_CALL!>.<!>length
        x<!UNSAFE_CALL!>.<!>length // Smart cast is not possible
    }
}
