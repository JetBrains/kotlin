package abstract


enum class MyEnum() {
    //properties
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val a: Int<!>
    val a1: Int = 1
    abstract val a2: Int
    abstract val a3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>1<!>

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var b: Int<!>                private set
    var b1: Int = 0;                         private set
    abstract var b2: Int      private set
    abstract var b3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; private set

    <!MUST_BE_INITIALIZED!>var c: Int<!>                set(v: Int) { $c = v }
    var c1: Int = 0;                         set(v: Int) { $c1 = v }
    abstract var c2: Int                     <!ABSTRACT_PROPERTY_WITH_SETTER!>set(v: Int) { $c2 = v }<!>
    abstract var c3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; <!ABSTRACT_PROPERTY_WITH_SETTER!>set(v: Int) { $c3 = v }<!>

    val e: Int                               get() = a
    val e1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;          get() = a
    abstract val e2: Int                     <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = a<!>
    abstract val e3: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>0<!>; <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = a<!>

    //methods
    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun f()<!>
    fun g() {}
    abstract fun h()
    <!ABSTRACT_FUNCTION_WITH_BODY!>abstract<!> fun j() {}

    //property accessors
    var i: Int                       <!ILLEGAL_MODIFIER!>abstract<!> get  <!ILLEGAL_MODIFIER!>abstract<!> set
    var i1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;  <!ILLEGAL_MODIFIER!>abstract<!> get  <!ILLEGAL_MODIFIER!>abstract<!> set

    var j: Int                       get() = i;    <!ILLEGAL_MODIFIER!>abstract<!> set
    var j1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;  get() = i;    <!ILLEGAL_MODIFIER!>abstract<!> set

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var k: Int<!>        <!ILLEGAL_MODIFIER!>abstract<!> set
    var k1: Int = 0;                 <!ILLEGAL_MODIFIER!>abstract<!> set

    var l: Int                       <!ILLEGAL_MODIFIER!>abstract<!> get  <!ILLEGAL_MODIFIER!>abstract<!> set
    var l1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;   <!ILLEGAL_MODIFIER!>abstract<!> get  <!ILLEGAL_MODIFIER!>abstract<!> set

    var n: Int                       <!ILLEGAL_MODIFIER!>abstract<!> get <!ILLEGAL_MODIFIER!>abstract<!> set(v: Int) {}
}