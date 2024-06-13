// LANGUAGE: +ProhibitSelfCallsInNestedObjects
// ISSUE: KT-25289

abstract class Base(val baseProp: String)

open class Foo1(val prop: Int, baseProp: String) : Base(baseProp) {
    companion object : Foo1(<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>prop<!>, <!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>baseProp<!>)
}

open class Foo2(val prop: Int, baseProp: String) : Base(baseProp) {
    companion object : Foo2(<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>this.<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>prop<!><!>, <!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>this.<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>baseProp<!><!>)
}

open class Foo3(val prop: Int, baseProp: String) : Base(baseProp) {
    companion object : Foo3(<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>Companion.<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>prop<!><!>, <!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>Companion.<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>baseProp<!><!>)
}

open class Foo4(val prop: Int, baseProp: String) : Base(baseProp) {
    object MyObject : Foo4(<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>MyObject.<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>prop<!><!>, <!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>MyObject.<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>baseProp<!><!>)
}

open class CheckNested(a: Any) {
    class Nested

    companion object : CheckNested(Nested()) // Nested() doesn't have receiver, so there will be no error
}

open class Foo5(val prop: Int) {
    object MyObject : Foo5(with(MyObject) { <!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>prop<!> })
}
