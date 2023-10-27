@<!UNRESOLVED_REFERENCE!><!SYNTAX!><!>myAnnotation<!> public
package illegal_modifiers

abstract class A() {
    <!INCOMPATIBLE_MODIFIERS!>abstract<!> <!INCOMPATIBLE_MODIFIERS!>final<!> fun f()
    abstract <!REDUNDANT_MODIFIER!>open<!> fun g()
    <!INCOMPATIBLE_MODIFIERS!>final<!> <!INCOMPATIBLE_MODIFIERS!>open<!> fun h() {}

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>open var r: String<!>
        get
        <!WRONG_MODIFIER_TARGET!>abstract<!> protected set
}

<!WRONG_MODIFIER_TARGET!>final<!> interface T {}

class FinalClass() {
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> fun foo() {}
    val i: Int = 1
        <!WRONG_MODIFIER_TARGET!>open<!> get(): Int = field
    var j: Int = 1
        <!WRONG_MODIFIER_TARGET!>open<!> set(v: Int) {}
}

<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>public<!> class C
<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>public<!> object D

//A sample annotation to check annotation usage in parameters.
annotation class annotated(val text: String = "not given")

//Check legal modifiers in constructor
class LegalModifier(val a: Int, @annotated private var b: String, @annotated vararg v: Int)

//Check illegal modifier in constructor parameters
class IllegalModifiers1(
    <!WRONG_MODIFIER_TARGET!>in<!>
    <!INCOMPATIBLE_MODIFIERS!>out<!>
    <!WRONG_MODIFIER_TARGET!>reified<!>
    <!WRONG_MODIFIER_TARGET!>enum<!>
    <!WRONG_MODIFIER_TARGET!>private<!>
    <!WRONG_MODIFIER_TARGET!>const<!>
    a: Int)

//Check multiple illegal modifiers in constructor
class IllegalModifiers2(<!WRONG_MODIFIER_TARGET!>private<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> a: Int)


//Check annotations with illegal modifiers in constructor
class IllegalModifiers3(@annotated <!WRONG_MODIFIER_TARGET!>public<!> <!WRONG_MODIFIER_TARGET!>abstract<!> b: String)

//Check annotations and vararg with illegal modifiers in constructor
class IllegalModifiers4(val a: Int, @annotated("a text") <!WRONG_MODIFIER_TARGET!>protected<!> vararg v: Int)

//Check illegal modifiers for functions and catch block
abstract class IllegalModifiers5() {

    //Check illegal modifier in function parameter
    abstract fun foo(<!WRONG_MODIFIER_TARGET!>public<!> a: Int, vararg v: String)

    //Check multiple illegal modifiers in function parameter
    abstract fun bar(<!WRONG_MODIFIER_TARGET!>public<!> <!WRONG_MODIFIER_TARGET!>abstract<!> a: Int, vararg v: String)

    //Check annotations with illegal modifiers
    abstract fun baz(@annotated("a text") <!WRONG_MODIFIER_TARGET!>public<!> <!WRONG_MODIFIER_TARGET!>abstract<!> a: Int)

    private fun qux() {

        //Check illegal modifier in catch block
        try {} catch (<!WRONG_MODIFIER_TARGET!>in<!> <!INCOMPATIBLE_MODIFIERS!>out<!> <!WRONG_MODIFIER_TARGET!>reified<!> <!WRONG_MODIFIER_TARGET!>enum<!> <!WRONG_MODIFIER_TARGET!>public<!> e: Exception) {}

        //Check multiple illegal modifiers in catch block
        try {} catch (<!WRONG_MODIFIER_TARGET!>in<!> <!INCOMPATIBLE_MODIFIERS!>out<!> <!WRONG_MODIFIER_TARGET!>reified<!> <!WRONG_MODIFIER_TARGET!>enum<!> <!WRONG_MODIFIER_TARGET!>abstract<!> <!WRONG_MODIFIER_TARGET!>public<!> e: Exception) {}

        //Check annotations with illegal modifiers
        try {} catch (@annotated("a text") <!WRONG_MODIFIER_TARGET!>abstract<!> <!WRONG_MODIFIER_TARGET!>public<!> e: Exception) {}
    }
}

//Check illegal modifiers on anonymous initializers
abstract class IllegalModifiers6() {
    <!WRONG_MODIFIER_TARGET!>public<!> init {}
    <!WRONG_MODIFIER_TARGET!>private<!> init {}
    <!WRONG_MODIFIER_TARGET!>protected<!> init {}
    <!WRONG_MODIFIER_TARGET!>vararg<!> init {}
    <!WRONG_MODIFIER_TARGET!>abstract<!> init {}
    <!WRONG_MODIFIER_TARGET!>open<!> init {}
    <!WRONG_MODIFIER_TARGET!>final<!> init {}

    <!WRONG_MODIFIER_TARGET!>public<!> <!WRONG_ANNOTATION_TARGET!>@annotated<!> init {}

    <!WRONG_MODIFIER_TARGET!>private<!> <!WRONG_ANNOTATION_TARGET!>@<!NOT_AN_ANNOTATION_CLASS!>IllegalModifiers6<!>()<!> init {}
}

// strange inappropriate modifiers usages
<!WRONG_MODIFIER_TARGET!>override<!>
<!WRONG_MODIFIER_TARGET!>out<!>
<!INCOMPATIBLE_MODIFIERS!>in<!>
<!WRONG_MODIFIER_TARGET!>vararg<!>
<!WRONG_MODIFIER_TARGET!>reified<!>
class IllegalModifiers7() {
    <!WRONG_MODIFIER_TARGET!>enum<!>
    <!WRONG_MODIFIER_TARGET!>inner<!>
    <!WRONG_MODIFIER_TARGET!>annotation<!>
    <!WRONG_MODIFIER_TARGET!>out<!>
    <!INCOMPATIBLE_MODIFIERS!>in<!>
    <!WRONG_MODIFIER_TARGET!>vararg<!>
    <!WRONG_MODIFIER_TARGET!>reified<!>
    val x = 1
    <!WRONG_MODIFIER_TARGET!>enum<!>
    <!WRONG_MODIFIER_TARGET!>inner<!>
    <!WRONG_MODIFIER_TARGET!>annotation<!>
    <!WRONG_MODIFIER_TARGET!>out<!>
    <!INCOMPATIBLE_MODIFIERS!>in<!>
    <!WRONG_MODIFIER_TARGET!>vararg<!>
    <!WRONG_MODIFIER_TARGET!>reified<!>
    <!WRONG_MODIFIER_TARGET!>const<!>
    fun foo() {}
}

// Secondary constructors
class IllegalModifiers8 {
    <!WRONG_MODIFIER_TARGET!>abstract<!>
    enum
    <!REDUNDANT_MODIFIER, WRONG_MODIFIER_TARGET!>open<!>
    <!WRONG_MODIFIER_TARGET!>inner<!>
    <!WRONG_MODIFIER_TARGET!>annotation<!>
    <!WRONG_MODIFIER_TARGET!>override<!>
    <!WRONG_MODIFIER_TARGET!>out<!>
    <!INCOMPATIBLE_MODIFIERS!>in<!>
    <!INCOMPATIBLE_MODIFIERS!>final<!>
    <!WRONG_MODIFIER_TARGET!>vararg<!>
    <!WRONG_MODIFIER_TARGET!>reified<!>
    <!INCOMPATIBLE_MODIFIERS!>const<!><!SYNTAX!><!>
    constructor() {}

    constructor(<!WRONG_MODIFIER_TARGET!>private<!> <!WRONG_MODIFIER_TARGET!>enum<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> x: Int) {}
}

class IllegalModifiers9 {
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>protected<!> constructor() {}
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>internal<!> constructor(x: Int) {}
}

// Illegal modifiers on primary constructor

class IllegalModifiers10
<!WRONG_MODIFIER_TARGET!>abstract<!>
<!WRONG_MODIFIER_TARGET!>enum<!>
<!REDUNDANT_MODIFIER, WRONG_MODIFIER_TARGET!>open<!>
<!WRONG_MODIFIER_TARGET!>inner<!>
<!WRONG_MODIFIER_TARGET!>annotation<!>
<!WRONG_MODIFIER_TARGET!>override<!>
<!WRONG_MODIFIER_TARGET!>out<!>
<!INCOMPATIBLE_MODIFIERS!>in<!>
<!INCOMPATIBLE_MODIFIERS!>final<!>
<!WRONG_MODIFIER_TARGET!>vararg<!>
<!WRONG_MODIFIER_TARGET!>reified<!>
<!INCOMPATIBLE_MODIFIERS!>const<!> constructor()

class IllegalModifiers11 <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>protected<!> constructor()

class Outer {
    <!INCOMPATIBLE_MODIFIERS!>inner<!> <!INCOMPATIBLE_MODIFIERS!>sealed<!> class Inner
}
