package abstract

class MyClass() {
    //properties
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val a: Int<!>
    val a1: Int = 1
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val a2: Int
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val a3: Int = 1

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var b: Int<!>                private set
    var b1: Int = 0;                         private set
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> var b2: Int      private set
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> var b3: Int = 0; private set

    <!MUST_BE_INITIALIZED!>var c: Int<!>                set(v: Int) { $c = v }
    var c1: Int = 0;                         set(v: Int) { $c1 = v }
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> var c2: Int      set(v: Int) { $c2 = v }
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> var c3: Int = 0; set(v: Int) { $c3 = v }

    val e: Int                               get() = a
    val e1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;          get() = a
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val e2: Int      get() = a
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val e3: Int = 0; get() = a

    //methods
    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun f()<!>
    fun g() {}
    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS!>abstract<!> fun h()
    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, ABSTRACT_FUNCTION_WITH_BODY!>abstract<!> fun j() {}

    //property accessors
    var i: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var i1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;  <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var j: Int                       get() = i;    <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var j1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;  get() = i;    <!WRONG_MODIFIER_TARGET!>abstract<!> set

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var k: Int<!>        <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var k1: Int = 0;                 <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var l: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var l1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;  <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var n: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> get <!WRONG_MODIFIER_TARGET!>abstract<!> set(v: Int) {}
}