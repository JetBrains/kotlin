fun foo() {}

class A {
    fun baz() {}
}

val bar = <!REFLECTION_TYPES_NOT_LOADED!>::<!>foo
val quux = A<!REFLECTION_TYPES_NOT_LOADED!>::<!>baz
