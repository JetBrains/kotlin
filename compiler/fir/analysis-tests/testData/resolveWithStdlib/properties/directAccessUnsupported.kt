// LANGUAGE: -DirectFieldOrDelegateAccess

var number: String
    <!UNSUPPORTED_FEATURE!>internal field = 10<!>
    get() = field.toString()
    set(newValue) {
        field = newValue.length
    }

fun updateNumber() {
    <!UNSUPPORTED_FEATURE!>number#field = 20<!>
    val rawValue: Int = <!UNSUPPORTED_FEATURE!>number#field<!>
}
