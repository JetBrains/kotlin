// !DIAGNOSTICS: -UNUSED_PARAMETER
class A1 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleResult(x: Int) {
    }
}

class A2 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleResult(x: Int, y: Int) {
    }
}

class A3 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleResult(x: Int, y: Continuation<Any>) {
    }
}

class A4 {
    operator fun handleResult(x: Int, y: Continuation<Nothing>) {
    }
}

class A5 {
    // TODO: Allow?
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleResult(x: Int, y: Continuation<Nothing>): Int = 1
}

class A6 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun <T> handleResult(x: Int, y: Continuation<Nothing>) {
    }

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleResult(x: String = "", y: Continuation<Nothing>) {}
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleResult(x: Double = 1.0, vararg y: Continuation<Nothing>) {}
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun handleResult(x: Double = 1.0, z: Int, y: Continuation<Nothing>) {}
}
