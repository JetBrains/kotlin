<!UNRESOLVED_REFERENCE!>myAnnotation<!> <!ILLEGAL_MODIFIER!>public<!> package illegal_modifiers

abstract class A() {
    <!INCOMPATIBLE_MODIFIERS!>abstract<!> <!INCOMPATIBLE_MODIFIERS!>final<!> fun f()
    abstract <!REDUNDANT_MODIFIER!>open<!> fun g()
    <!INCOMPATIBLE_MODIFIERS!>final<!> <!INCOMPATIBLE_MODIFIERS!>open<!> fun h() {}

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>open var r: String<!>
    get
    <!ILLEGAL_MODIFIER!>abstract<!> protected set
}

<!TRAIT_CAN_NOT_BE_FINAL!>final<!> trait T {}

class FinalClass() {
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> fun foo() {}
    val i: Int = 1
        <!ILLEGAL_MODIFIER!>open<!> get(): Int = $i
    var j: Int = 1
        <!ILLEGAL_MODIFIER!>open<!> set(v: Int) {}
}

<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>public<!> class C
<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>public<!> object D

//A sample annotation to check annotation usage in parameters.
annotation class annotated(val text: String = "not given")

//Check legal modifiers in constructor
class LegalModifier(val a: Int, annotated private var b: String, annotated vararg <!UNUSED_PARAMETER!>v<!>: Int)

//Check illegal modifier in constructor parameters
class IllegalModifiers1(<!ILLEGAL_MODIFIER!>private<!> <!UNUSED_PARAMETER!>a<!>: Int)

//Check multiple illegal modifiers in constructor
class IllegalModifiers2(<!ILLEGAL_MODIFIER!>private<!> <!ILLEGAL_MODIFIER!>abstract<!> <!UNUSED_PARAMETER!>a<!>: Int)


//Check annotations with illegal modifiers in constructor
class IllegalModifiers3(annotated <!ILLEGAL_MODIFIER!>public<!> <!ILLEGAL_MODIFIER!>abstract<!> <!UNUSED_PARAMETER!>b<!>: String)

//Check annotations and vararg with illegal modifiers in constructor
class IllegalModifiers4(val a: Int, annotated("a text") <!ILLEGAL_MODIFIER!>protected<!> vararg <!UNUSED_PARAMETER!>v<!>: Int)

//Check illegal modifiers for functions and catch block
abstract class IllegalModifiers5() {

  //Check illegal modifier in function parameter
  abstract fun foo(<!ILLEGAL_MODIFIER!>public<!> a: Int, vararg v: String)

  //Check multiple illegal modifiers in function parameter
  abstract fun bar(<!ILLEGAL_MODIFIER!>public<!> <!ILLEGAL_MODIFIER!>abstract<!> a: Int, vararg v: String)

  //Check annotations with illegal modifiers
  abstract fun baz(annotated("a text") <!ILLEGAL_MODIFIER!>public<!> <!ILLEGAL_MODIFIER!>abstract<!> a: Int)

  private fun qux() {

    //Check illegal modifier in catch block
    try {} catch (<!ILLEGAL_MODIFIER!>public<!> e: Exception) {}

    //Check multiple illegal modifiers in catch block
    try {} catch (<!ILLEGAL_MODIFIER!>abstract<!> <!ILLEGAL_MODIFIER!>public<!> e: Exception) {}

    //Check annotations with illegal modifiers
    try {} catch (annotated("a text") <!ILLEGAL_MODIFIER!>abstract<!> <!ILLEGAL_MODIFIER!>public<!> e: Exception) {}
  }
}

//Check illegal modifiers on anonymous initializers
abstract class IllegalModifiers6() {
    <!ILLEGAL_MODIFIER!>public<!> init {}
    <!ILLEGAL_MODIFIER!>private<!> init {}
    <!ILLEGAL_MODIFIER!>protected<!> init {}
    <!ILLEGAL_MODIFIER!>vararg<!> init {}
    <!ILLEGAL_MODIFIER!>abstract<!> init {}
    <!ILLEGAL_MODIFIER!>open<!> init {}
    <!ILLEGAL_MODIFIER!>final<!> init {}

    <!ILLEGAL_MODIFIER!>public<!> annotated init {}

    <!ILLEGAL_MODIFIER!>private<!> <!NOT_AN_ANNOTATION_CLASS!>IllegalModifiers6()<!> init {}
}