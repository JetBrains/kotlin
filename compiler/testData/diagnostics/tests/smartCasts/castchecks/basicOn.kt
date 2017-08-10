// !LANGUAGE: +SafeCastCheckBoundSmartCasts

interface SomeClass {
    val data: Any?
}

interface SomeSubClass : SomeClass {
    val foo: Any?
}

fun g(a: SomeClass?) {
    if (a as? SomeSubClass != null) {
        // 'a' can be cast to SomeSubClass
        <!DEBUG_INFO_SMARTCAST!>a<!>.hashCode()
        <!DEBUG_INFO_SMARTCAST!>a<!>.foo
        (a as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        (a <!USELESS_CAST!>as SomeSubClass<!>).foo
    }
    val b = (a as? SomeSubClass)?.foo
    if (b != null) {
        // 'a' can be cast to SomeSubClass
        <!DEBUG_INFO_SMARTCAST!>a<!>.hashCode()
        <!DEBUG_INFO_SMARTCAST!>a<!>.foo
        (a as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        (a <!USELESS_CAST!>as SomeSubClass<!>).foo
    }
    val c = a as? SomeSubClass
    if (c != null) {
        // 'a' and 'c' can be cast to SomeSubClass
        <!DEBUG_INFO_SMARTCAST!>a<!>.hashCode()
        <!DEBUG_INFO_SMARTCAST!>a<!>.foo
        (a as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        <!DEBUG_INFO_SMARTCAST!>c<!>.hashCode()
        <!DEBUG_INFO_SMARTCAST!>c<!>.foo
    }
}