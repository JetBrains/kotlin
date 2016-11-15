// !DIAGNOSTICS: -UNUSED_PARAMETER
class A1 {
    operator fun interceptResume(x: () -> Unit) {}
}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun A1.interceptResume(x: () -> Unit) {}

class A2 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun interceptResume(x: () -> Unit, w: Int) {}
}

class A3 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun interceptResume(x: String.() -> Unit) = 1
}

class A4 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun interceptResume(x: (String) -> Unit) {}
}

class A5 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun interceptResume(x: () -> Int) {}
}

class A6 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun <T> interceptResume(x: () -> Unit) {}
}

class A7 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun interceptResume(x: () -> Unit = {}) {}
}

class A8 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun interceptResume(vararg x: () -> Unit) {}
}

class A9 {
    inline operator fun interceptResume(x: () -> Unit) {}
}
