class Your

fun Your.foo() = Any()

fun <T> T?.let(f: (T) -> Unit) {
    if (this != null) f(this)
}

fun test(your: Your?) {
    (your?.foo() <!USELESS_CAST!>as? Any<!>)?.let {}
    // strange smart cast to 'Your' at this point
    your<!UNSAFE_CALL!>.<!>hashCode()
}
