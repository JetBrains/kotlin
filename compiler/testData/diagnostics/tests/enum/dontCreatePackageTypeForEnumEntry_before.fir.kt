// !LANGUAGE: -NestedClassesInEnumEntryShouldBeInner

enum class E {
    FIRST,
    SECOND {
        class A
    };
}

val foo: Any.() -> Unit = {}

fun f1() = E.FIRST.<!UNRESOLVED_REFERENCE!>foo<!>()
fun f2() = E.FIRST.(<!UNRESOLVED_REFERENCE!>foo<!>)()
fun f3() = E.SECOND.<!UNRESOLVED_REFERENCE!>foo<!>()
fun f4() = E.SECOND.(<!UNRESOLVED_REFERENCE!>foo<!>)()
fun f5() = E.SECOND.A()
