// !LANGUAGE: -SafeCastCheckBoundSmartCasts

interface SomeClass {
    val data: Any?
}

interface SomeSubClass : SomeClass {
    val foo: Any?
}

fun g(a: SomeClass?) {
    if (a as? SomeSubClass != null) {
        // 'a' can be cast to SomeSubClass
        a.hashCode()
        a.foo
        (a as? SomeSubClass).<!INAPPLICABLE_CANDIDATE!>foo<!>
        (a as SomeSubClass).foo
    }
    val b = (a as? SomeSubClass)?.foo
    if (b != null) {
        // 'a' can be cast to SomeSubClass
        a.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
        a.<!UNRESOLVED_REFERENCE!>foo<!>
        (a as? SomeSubClass).<!INAPPLICABLE_CANDIDATE!>foo<!>
        (a as SomeSubClass).foo
    }
    val c = a as? SomeSubClass
    if (c != null) {
        // 'a' and 'c' can be cast to SomeSubClass
        a.hashCode()
        a.foo
        (a as? SomeSubClass).<!INAPPLICABLE_CANDIDATE!>foo<!>
        c.hashCode()
        c.foo
    }
}