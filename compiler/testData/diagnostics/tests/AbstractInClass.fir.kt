package abstract

class MyClass() {
    //properties
    val a: Int
    val a1: Int = 1
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val a2: Int
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val a3: Int = 1

    var b: Int                private set
    var b1: Int = 0;                         private set
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> var b2: Int      private set
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> var b3: Int = 0; private set

    var c: Int                set(v: Int) { field = v }
    var c1: Int = 0;                         set(v: Int) { field = v }
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> var c2: Int      set(v: Int) { field = v }
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> var c3: Int = 0; set(v: Int) { field = v }

    val e: Int                               get() = a
    val e1: Int = 0;          get() = a
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val e2: Int      get() = a
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val e3: Int = 0; get() = a

    //methods
    fun f()
    fun g() {}
    abstract fun h()
    abstract fun j() {}

    //property accessors
    var i: Int                       abstract get  abstract set
    var i1: Int = 0;  abstract get  abstract set

    var j: Int                       get() = i;    abstract set
    var j1: Int = 0;  get() = i;    abstract set

    var k: Int        abstract set
    var k1: Int = 0;                 abstract set

    var l: Int                       abstract get  abstract set
    var l1: Int = 0;  abstract get  abstract set

    var n: Int                       abstract get abstract set(v: Int) {}
}