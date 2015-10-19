fun foo(y: String?) {
    var x: String? = ""
    if (x != null) {
        with(y?.let { x = null; it }) {
            this<!UNSAFE_CALL!>.<!>length
            x<!UNSAFE_CALL!>.<!>length
        }
        x<!UNSAFE_CALL!>.<!>length
    }
}
