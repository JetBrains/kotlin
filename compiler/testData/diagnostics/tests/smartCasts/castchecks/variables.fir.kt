// !WITH_NEW_INFERENCE
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
        a.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
        a.<!UNRESOLVED_REFERENCE!>foo<!>
        (a as? SomeSubClass).<!INAPPLICABLE_CANDIDATE!>foo<!>
        (a as SomeSubClass).foo
    }
    var c = a as? SomeSubClass
    c = Impl
    if (c != null) {
        // 'a' cannot be cast to SomeSubClass
        a.hashCode()
        a.foo
        (a as? SomeSubClass).<!INAPPLICABLE_CANDIDATE!>foo<!>
        c.hashCode()
        c.foo
    }
}

fun f(a: SomeClass?) {
    var aa = a

    if (aa as? SomeSubClass != null) {
        aa = null
        // 'aa' cannot be cast to SomeSubClass
        aa.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
        aa.<!UNRESOLVED_REFERENCE!>foo<!>
        (aa as? SomeSubClass).<!INAPPLICABLE_CANDIDATE!>foo<!>
        (aa as SomeSubClass).foo
    }
    val b = (aa as? SomeSubClass)?.foo
    aa = null
    if (b != null) {
        // 'aa' cannot be cast to SomeSubClass
        aa.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
        aa.<!UNRESOLVED_REFERENCE!>foo<!>
        (aa as? SomeSubClass).<!INAPPLICABLE_CANDIDATE!>foo<!>
        (aa as SomeSubClass).foo
    }
    aa = a
    val c = aa as? SomeSubClass
    if (c != null) {
        // 'c' can be cast to SomeSubClass
        aa.hashCode()
        aa.foo
        (aa as? SomeSubClass).<!INAPPLICABLE_CANDIDATE!>foo<!>
        c.hashCode()
        c.foo
    }
}