package abstract

interface MyTrait {
    //properties
    val a: Int
    val a1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
    abstract val a2: Int
    abstract val a3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>1<!>

    var b: Int                                                                                              <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set
    var b1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;                                                <!PRIVATE_SETTER_FOR_OPEN_PROPERTY!>private<!> set
    abstract var b2: Int                                               <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set
    abstract var b3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set

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
    var i: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var i1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;  <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var j: Int                       get() = i;    <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var j1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;  get() = i;    <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var k: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var k1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;  <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var l: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var l1: Int = <!PROPERTY_INITIALIZER_IN_INTERFACE!>0<!>;  <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var n: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> get <!WRONG_MODIFIER_TARGET!>abstract<!> set(v: Int) {}
}