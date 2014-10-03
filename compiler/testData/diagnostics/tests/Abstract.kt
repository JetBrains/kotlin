// FILE: b.kt
package MyPackage
    //properties

    <!MUST_BE_INITIALIZED!>val a: Int<!>
    val a1: Int = 1
    <!MUST_BE_INITIALIZED!><!ILLEGAL_MODIFIER!>abstract<!> val a2: Int<!>
    <!ILLEGAL_MODIFIER!>abstract<!> val a3: Int = 1

    <!MUST_BE_INITIALIZED!>var b: Int<!>                private set
    var b1: Int = 0;                         private set
    <!MUST_BE_INITIALIZED!><!ILLEGAL_MODIFIER!>abstract<!> var b2: Int<!>      private set
    <!ILLEGAL_MODIFIER!>abstract<!> var b3: Int = 0; private set

    <!MUST_BE_INITIALIZED!>var c: Int<!>                set(v: Int) { $c = v }
    var c1: Int = 0;                         set(v: Int) { $c1 = v }
    <!MUST_BE_INITIALIZED!><!ILLEGAL_MODIFIER!>abstract<!> var c2: Int<!>      set(v: Int) { $c2 = v }
    <!ILLEGAL_MODIFIER!>abstract<!> var c3: Int = 0; set(v: Int) { $c3 = v }

    val e: Int                               get() = a
    val e1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;          get() = a
    <!ILLEGAL_MODIFIER!>abstract<!> val e2: Int      get() = a
    <!ILLEGAL_MODIFIER!>abstract<!> val e3: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>; get() = a

    //methods

    <!NON_MEMBER_FUNCTION_NO_BODY!>fun f()<!>
    fun g() {}
    <!ILLEGAL_MODIFIER!>abstract<!> fun h()
    <!ILLEGAL_MODIFIER!>abstract<!> fun j() {}

    //property accessors

    var i: Int                       <!ILLEGAL_MODIFIER!>abstract<!> get  <!ILLEGAL_MODIFIER!>abstract<!> set
    var i1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;  <!ILLEGAL_MODIFIER!>abstract<!> get  <!ILLEGAL_MODIFIER!>abstract<!> set

    var j: Int                       get() = i;    <!ILLEGAL_MODIFIER!>abstract<!> set
    var j1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;  get() = i;    <!ILLEGAL_MODIFIER!>abstract<!> set

    <!MUST_BE_INITIALIZED!>var k: Int<!>        <!ILLEGAL_MODIFIER!>abstract<!> set
    var k1: Int = 0;                 <!ILLEGAL_MODIFIER!>abstract<!> set

    var l: Int                       <!ILLEGAL_MODIFIER!>abstract<!> get  <!ILLEGAL_MODIFIER!>abstract<!> set
    var l1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;  <!ILLEGAL_MODIFIER!>abstract<!> get  <!ILLEGAL_MODIFIER!>abstract<!> set

    var n: Int                       <!ILLEGAL_MODIFIER!>abstract<!> get <!ILLEGAL_MODIFIER!>abstract<!> set(v: Int) {}

// FILE: c.kt
//creating an instance
abstract class B1(
    val i: Int,
    val s: String
) {
}

class B2() : B1(1, "r") {}

abstract class B3(i: Int) {
}

fun foo(<!UNUSED_PARAMETER!>c<!>: B3) {
    val <!UNUSED_VARIABLE!>a<!> = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>B3(1)<!>
    val <!UNUSED_VARIABLE!>b<!> = <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>B1(2, "s")<!>
}