// !DIAGNOSTICS: -UNUSED_PARAMETER
class A1 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleException(x: Throwable) {
    }
}

class A2 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleException(x: Throwable, y: Int) {
    }
}

class A3 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleException(x: Throwable, y: Continuation<Any>) {
    }
}

class A4 {
    operator fun handleException(x: Throwable, y: Continuation<Nothing>) {
    }
}

class A5 {
    // TODO: Allow?
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleException(x: Throwable, y: Continuation<Nothing>): Int = 1
}

class A6 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleException(x: Throwable = Exception(), y: Continuation<Nothing>) {}
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleException(x: Throwable, vararg y: Continuation<Nothing>) {}
}

class A7 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun <T> handleException(x: Throwable, y: Continuation<Nothing>) {
    }
}
