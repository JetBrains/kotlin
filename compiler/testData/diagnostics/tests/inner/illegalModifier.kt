<!ILLEGAL_MODIFIER!>inner<!> fun foo() {}
<!ILLEGAL_MODIFIER!>inner<!> val prop = 42

<!ILLEGAL_MODIFIER!>inner<!> class A
<!ILLEGAL_MODIFIER!>inner<!> interface B
<!ILLEGAL_MODIFIER!>inner<!> object C

class D {
    inner class E
    <!ILLEGAL_MODIFIER!>inner<!> interface F
    <!ILLEGAL_MODIFIER!>inner<!> object G
    <!ILLEGAL_MODIFIER!>inner<!> enum class R
    <!ILLEGAL_MODIFIER!>inner<!> annotation class S
    <!ILLEGAL_MODIFIER!>inner<!> companion object
}

enum class H {
    <!ILLEGAL_MODIFIER!>inner<!> I {
        inner class II
    };
    
    inner class J
}

interface K {
    <!INNER_CLASS_IN_TRAIT!>inner<!> class L
}

object N {
    <!INNER_CLASS_IN_OBJECT!>inner<!> class O
}

class P {
    companion object {
        <!INNER_CLASS_IN_OBJECT!>inner<!> class Q
    }
}

val R = object {
    inner class S
}
