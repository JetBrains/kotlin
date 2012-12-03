<!ILLEGAL_MODIFIER!>inner<!> fun foo() {}
<!ILLEGAL_MODIFIER!>inner<!> val prop = 42

<!ILLEGAL_MODIFIER!>inner<!> class A
<!ILLEGAL_MODIFIER!>inner<!> trait B
<!ILLEGAL_MODIFIER!>inner<!> object C

class D {
    inner class E
    <!ILLEGAL_MODIFIER!>inner<!> trait F
    <!ILLEGAL_MODIFIER!>inner<!> object G
    <!ILLEGAL_MODIFIER!>inner<!> enum class R
    <!ILLEGAL_MODIFIER!>inner<!> annotation class S
    <!ILLEGAL_MODIFIER!>inner<!> class object
}

enum class H {
    <!ILLEGAL_MODIFIER!>inner<!> I {
        inner class II
    }
    
    inner class J
}

trait K {
    <!ILLEGAL_MODIFIER!>inner<!> class L
}

object N {
    inner class O
}

class P {
    class object {
        inner class Q
    }
}

val R = object {
    inner class S
}
