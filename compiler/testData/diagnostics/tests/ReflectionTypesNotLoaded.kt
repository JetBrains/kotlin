fun foo() {}

class A(val prop: Any) {
    fun baz() {}
}

val topLevelFun = <!REFLECTION_TYPES_NOT_LOADED!>::<!>foo
val memberFun = A<!REFLECTION_TYPES_NOT_LOADED!>::<!>baz

val classLiteral = A<!REFLECTION_TYPES_NOT_LOADED!>::<!>class
val property = A<!REFLECTION_TYPES_NOT_LOADED!>::<!>prop
