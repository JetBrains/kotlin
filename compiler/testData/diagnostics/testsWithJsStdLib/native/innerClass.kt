// FIR_IDENTICAL
external class C {
    inner class <!WRONG_EXTERNAL_DECLARATION!>Inner<!>
}

external enum class E {
    X;

    inner class <!WRONG_EXTERNAL_DECLARATION!>Inner<!>
}