package abstract

abstract class MyAbstractClass() {
    //properties
    val a: Int
    val a1: Int = 1
    abstract val a2: Int
    abstract val a3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>1<!>

    var b: Int                private set
    var b1: Int = 0;                         private set
    abstract var b2: Int      private set
    abstract var b3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; private set

    var c: Int                set(v: Int) { field = v }
    var c1: Int = 0;                         set(v: Int) { field = v }
    abstract var c2: Int                     <!ABSTRACT_PROPERTY_WITH_SETTER!>set(v: Int) { field = v }<!>
    abstract var c3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; <!ABSTRACT_PROPERTY_WITH_SETTER!>set(v: Int) { field = v }<!>

    val e: Int                               get() = a
    val e1: Int = 0;          get() = a
    abstract val e2: Int                     <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = a<!>
    abstract val e3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = a<!>

    //methods
    fun f()
    fun g() {}
    abstract fun h()
    abstract fun j() {}

    //property accessors
    var i: Int                       abstract get  abstract set
    var i1: Int = 0;  abstract get  abstract set

    var j: Int                       get() = i;    abstract set
    var j1: Int                      get() = i;    abstract set

    var k: Int        abstract set
    var k1: Int = 0;                 abstract set

    var l: Int                       abstract get  abstract set
    var l1: Int = 0;  abstract get  abstract set

    var n: Int                       abstract get abstract set(v: Int) {}
}