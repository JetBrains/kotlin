class Your

fun Your.foo() = Any()

fun <T> T?.let(f: (T) -> Unit) {
    if (this != null) f(this)
}

fun test(your: Your?) {
    (your?.foo() as? Any)?.let {}
    // strange smart cast to 'Your' at this point
    your.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
}