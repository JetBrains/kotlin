package abstract

interface MyTrait {
    //properties
    val a: Int
    val a1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
    abstract val a2: Int
    abstract val a3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>1<!>

    var b: Int                                                                                              private set
    var b1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;                                                private set
    abstract var b2: Int                                               private set
    abstract var b3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; private set

    <!BACKING_FIELD_IN_INTERFACE!>var c: Int<!>                                   set(v: Int) { field = v }
    <!BACKING_FIELD_IN_INTERFACE!>var c1: Int<!> = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;              set(v: Int) { field = v }
    abstract var c2: Int                     <!ABSTRACT_PROPERTY_WITH_SETTER!>set(v: Int) { field = v }<!>
    abstract var c3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; <!ABSTRACT_PROPERTY_WITH_SETTER!>set(v: Int) { field = v }<!>

    val e: Int                                                  get() = a
    val e1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;                             get() = a
    abstract val e2: Int                     <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = a<!>
    abstract val e3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = a<!>

    //methods
    fun f()
    fun g() {}
    abstract fun h()
    <!ABSTRACT_FUNCTION_WITH_BODY!>abstract<!> fun j() {}

    //property accessors
    var i: Int                       abstract get  abstract set
    var i1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;  abstract get  abstract set

    <!BACKING_FIELD_IN_INTERFACE!>var j: Int<!>                       get() = i;    abstract set
    <!BACKING_FIELD_IN_INTERFACE!>var j1: Int<!> = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;  get() = i;    abstract set

    var k: Int                       abstract set
    var k1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;  abstract set

    var l: Int                       abstract get  abstract set
    var l1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;  abstract get  abstract set

    <!BACKING_FIELD_IN_INTERFACE!>var n: Int<!>                       abstract get abstract set(v: Int) {}
}