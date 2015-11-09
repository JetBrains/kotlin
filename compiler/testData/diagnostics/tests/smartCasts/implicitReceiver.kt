open class A {
    class B : A() {
        val a = "FAIL"
    }

    fun foo(): String {
        if (this is B) return <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>a<!>
        return "OK"
    }
}

fun A?.bar() {
    if (this != null) <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>foo<!>()
}

fun A.gav() = if (this is A.B) <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>a<!> else ""

class C {
    fun A?.complex(): String {
        if (this is A.B) return <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>a<!>
        else if (this != null) return <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>foo<!>()
        else return ""
    }
}
