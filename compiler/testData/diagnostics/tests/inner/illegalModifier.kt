<!WRONG_MODIFIER_TARGET!>inner<!> fun foo() {}
<!WRONG_MODIFIER_TARGET!>inner<!> val prop = 42

<!WRONG_MODIFIER_TARGET!>inner<!> class A
<!WRONG_MODIFIER_TARGET!>inner<!> interface B
<!WRONG_MODIFIER_TARGET!>inner<!> object C

class D {
    inner class E
    <!WRONG_MODIFIER_TARGET!>inner<!> interface F
    <!WRONG_MODIFIER_TARGET!>inner<!> object G
    <!WRONG_MODIFIER_TARGET!>inner<!> enum class R
    <!WRONG_MODIFIER_TARGET!>inner<!> annotation class S
    <!WRONG_MODIFIER_TARGET!>inner<!> companion object
}

enum class H {
    <!WRONG_MODIFIER_TARGET!>inner<!> I {
        inner class II
    };
    
    inner class J
}

interface K {
    <!WRONG_MODIFIER_TARGET!>inner<!> class L
}

object N {
    <!WRONG_MODIFIER_TARGET!>inner<!> class O
}

class P {
    companion object {
        <!WRONG_MODIFIER_TARGET!>inner<!> class Q
    }
}

val R = object {
    inner class S
}
