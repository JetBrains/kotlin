package abstract

interface MyTrait {
    //properties
    val a: Int
    val a1: Int = <!PROPERTY_INITIALIZER_IN_TRAIT!>1<!>
    <!ABSTRACT_MODIFIER_IN_TRAIT!>abstract<!> val a2: Int
    <!ABSTRACT_MODIFIER_IN_TRAIT!>abstract<!> val a3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>1<!>

    var b: Int                                                  private set
    var b1: Int = <!PROPERTY_INITIALIZER_IN_TRAIT!>0<!>;                             private set
    <!ABSTRACT_MODIFIER_IN_TRAIT!>abstract<!> var b2: Int                     private set
    <!ABSTRACT_MODIFIER_IN_TRAIT!>abstract<!> var b3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; private set

    <!BACKING_FIELD_IN_TRAIT!>var c: Int<!>                                   set(v: Int) { $c = v }
    <!BACKING_FIELD_IN_TRAIT!>var c1: Int<!> = <!PROPERTY_INITIALIZER_IN_TRAIT!>0<!>;              set(v: Int) { $c1 = v }
    <!ABSTRACT_MODIFIER_IN_TRAIT!>abstract<!> var c2: Int                     <!ABSTRACT_PROPERTY_WITH_SETTER!>set(v: Int) { $c2 = v }<!>
    <!ABSTRACT_MODIFIER_IN_TRAIT!>abstract<!> var c3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; <!ABSTRACT_PROPERTY_WITH_SETTER!>set(v: Int) { $c3 = v }<!>

    val e: Int                                                  get() = a
    val e1: Int = <!PROPERTY_INITIALIZER_IN_TRAIT!>0<!>;                             get() = a
    <!ABSTRACT_MODIFIER_IN_TRAIT!>abstract<!> val e2: Int                     <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = a<!>
    <!ABSTRACT_MODIFIER_IN_TRAIT!>abstract<!> val e3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = a<!>

    //methods
    fun f()
    fun g() {}
    <!ABSTRACT_MODIFIER_IN_TRAIT!>abstract<!> fun h()
    <!ABSTRACT_MODIFIER_IN_TRAIT, ABSTRACT_FUNCTION_WITH_BODY!>abstract<!> fun j() {}

    //property accessors
    var i: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var i1: Int = <!PROPERTY_INITIALIZER_IN_TRAIT!>0<!>;  <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var j: Int                       get() = i;    <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var j1: Int = <!PROPERTY_INITIALIZER_IN_TRAIT!>0<!>;  get() = i;    <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var k: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var k1: Int = <!PROPERTY_INITIALIZER_IN_TRAIT!>0<!>;  <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var l: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set
    var l1: Int = <!PROPERTY_INITIALIZER_IN_TRAIT!>0<!>;  <!WRONG_MODIFIER_TARGET!>abstract<!> get  <!WRONG_MODIFIER_TARGET!>abstract<!> set

    var n: Int                       <!WRONG_MODIFIER_TARGET!>abstract<!> get <!WRONG_MODIFIER_TARGET!>abstract<!> set(v: Int) {}
}