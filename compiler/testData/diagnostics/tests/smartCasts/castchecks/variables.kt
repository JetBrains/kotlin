// ISSUE: KT-56744
// !LANGUAGE: +SafeCastCheckBoundSmartCasts
interface SomeClass {
    val data: Any?
}

interface SomeSubClass : SomeClass {
    val foo: Any?
}

object Impl : SomeSubClass {
    override val data = ""
    override val foo = 42
}

fun g(a: SomeClass?) {
    var b = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>(a as? SomeSubClass)?.foo<!>
    b = "Hello"
    if (<!SENSELESS_COMPARISON!>b != null<!>) {
        // 'a' cannot be cast to SomeSubClass!
        a<!UNSAFE_CALL!>.<!>hashCode()
        a.<!UNRESOLVED_REFERENCE!>foo<!>
        (a as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        (a as SomeSubClass).foo
    }
    var c = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>a as? SomeSubClass<!>
    c = Impl
    if (<!SENSELESS_COMPARISON!>c != null<!>) {
        // 'a' cannot be cast to SomeSubClass
        a<!UNSAFE_CALL!>.<!>hashCode()
        a.<!UNRESOLVED_REFERENCE!>foo<!>
        (a as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        <!DEBUG_INFO_SMARTCAST!>c<!>.hashCode()
        <!DEBUG_INFO_SMARTCAST!>c<!>.foo
    }
}

fun f(a: SomeClass?) {
    var aa = a

    if (aa as? SomeSubClass != null) {
        aa = null
        // 'aa' cannot be cast to SomeSubClass
        aa<!UNSAFE_CALL!>.<!>hashCode()
        aa.<!UNRESOLVED_REFERENCE!>foo<!>
        (<!DEBUG_INFO_CONSTANT!>aa<!> as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        (<!ALWAYS_NULL!>aa<!> as SomeSubClass).foo
    }
    val b = (aa as? SomeSubClass)?.foo
    aa = null
    if (b != null) {
        // 'aa' cannot be cast to SomeSubClass
        aa<!UNSAFE_CALL!>.<!>hashCode()
        aa.<!UNRESOLVED_REFERENCE!>foo<!>
        (<!DEBUG_INFO_CONSTANT!>aa<!> as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        (<!ALWAYS_NULL!>aa<!> as SomeSubClass).foo
    }
    aa = a
    val c = aa as? SomeSubClass
    if (c != null) {
        // 'c' can be cast to SomeSubClass
        aa<!UNSAFE_CALL!>.<!>hashCode()
        aa.<!UNRESOLVED_REFERENCE!>foo<!>
        (aa as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        <!DEBUG_INFO_SMARTCAST!>c<!>.hashCode()
        <!DEBUG_INFO_SMARTCAST!>c<!>.foo
    }
}
