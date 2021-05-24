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
    var b = (a as? SomeSubClass)?.foo
    b = "Hello"
    if (b != null) {
        // 'a' cannot be cast to SomeSubClass!
        a<!UNSAFE_CALL!>.<!>hashCode()
        a.<!UNRESOLVED_REFERENCE!>foo<!>
        (a as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        (a as SomeSubClass).foo
    }
    var c = a as? SomeSubClass
    c = Impl
    if (c != null) {
        // 'a' cannot be cast to SomeSubClass
        a<!UNSAFE_CALL!>.<!>hashCode()
        a.<!UNRESOLVED_REFERENCE!>foo<!>
        (a as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        c.hashCode()
        c.foo
    }
}

fun f(a: SomeClass?) {
    var aa = a

    if (aa as? SomeSubClass != null) {
        aa = null
        // 'aa' cannot be cast to SomeSubClass
        aa<!UNSAFE_CALL!>.<!>hashCode()
        aa.<!UNRESOLVED_REFERENCE!>foo<!>
        (aa as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        (aa as SomeSubClass).foo
    }
    val b = (aa as? SomeSubClass)?.foo
    aa = null
    if (b != null) {
        // 'aa' cannot be cast to SomeSubClass
        aa<!UNSAFE_CALL!>.<!>hashCode()
        aa.<!UNRESOLVED_REFERENCE!>foo<!>
        (aa as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        (aa as SomeSubClass).foo
    }
    aa = a
    val c = aa as? SomeSubClass
    if (c != null) {
        // 'c' can be cast to SomeSubClass
        aa.hashCode()
        aa.foo
        (aa as? SomeSubClass)<!UNSAFE_CALL!>.<!>foo
        c.hashCode()
        c.foo
    }
}
