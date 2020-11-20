@<!UNRESOLVED_REFERENCE!><!SYNTAX!><!>myAnnotation<!> public
package illegal_modifiers

abstract class A() {
    <!INCOMPATIBLE_MODIFIERS!>abstract<!> <!INCOMPATIBLE_MODIFIERS!>final<!> fun f()
    abstract <!REDUNDANT_MODIFIER!>open<!> fun g()
    <!INCOMPATIBLE_MODIFIERS!>final<!> <!INCOMPATIBLE_MODIFIERS!>open<!> fun h() {}

    open var r: String
    get
    abstract protected set
}

final interface T {}

class FinalClass() {
    open fun foo() {}
    val i: Int = 1
        open get(): Int = field
    var j: Int = 1
        open set(v: Int) {}
}

<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>public<!> class C
<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>public<!> object D

//A sample annotation to check annotation usage in parameters.
annotation class annotated(val text: String = "not given")

//Check legal modifiers in constructor
class LegalModifier(val a: Int, @annotated private var b: String, @annotated vararg v: Int)

//Check illegal modifier in constructor parameters
class IllegalModifiers1(
        <!INCOMPATIBLE_MODIFIERS!>in<!>
        <!INCOMPATIBLE_MODIFIERS!>out<!>
        reified
        enum
        private
        const
        a: Int)

//Check multiple illegal modifiers in constructor
class IllegalModifiers2(<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> a: Int)


//Check annotations with illegal modifiers in constructor
class IllegalModifiers3(@annotated public abstract b: String)

//Check annotations and vararg with illegal modifiers in constructor
class IllegalModifiers4(val a: Int, @annotated("a text") protected vararg v: Int)

//Check illegal modifiers for functions and catch block
abstract class IllegalModifiers5() {

  //Check illegal modifier in function parameter
  abstract fun foo(public a: Int, vararg v: String)

  //Check multiple illegal modifiers in function parameter
  abstract fun bar(public abstract a: Int, vararg v: String)

  //Check annotations with illegal modifiers
  abstract fun baz(@annotated("a text") public abstract a: Int)

  private fun qux() {

    //Check illegal modifier in catch block
    try {} catch (<!INCOMPATIBLE_MODIFIERS!>in<!> <!INCOMPATIBLE_MODIFIERS!>out<!> reified enum public e: Exception) {}

    //Check multiple illegal modifiers in catch block
    try {} catch (<!INCOMPATIBLE_MODIFIERS!>in<!> <!INCOMPATIBLE_MODIFIERS!>out<!> reified enum abstract public e: Exception) {}

    //Check annotations with illegal modifiers
    try {} catch (@annotated("a text") abstract public e: Exception) {}
  }
}

//Check illegal modifiers on anonymous initializers
abstract class IllegalModifiers6() {
    public init {}
    private init {}
    protected init {}
    vararg init {}
    abstract init {}
    open init {}
    final init {}

    public @annotated init {}

    private @IllegalModifiers6() init {}
}

// strange inappropriate modifiers usages
override
<!INCOMPATIBLE_MODIFIERS!>out<!>
<!INCOMPATIBLE_MODIFIERS!>in<!>
vararg
reified
class IllegalModifiers7() {
    enum
    inner
    annotation
    <!INCOMPATIBLE_MODIFIERS!>out<!>
    <!INCOMPATIBLE_MODIFIERS!>in<!>
    vararg
    reified
    val x = 1
    enum
    inner
    annotation
    <!INCOMPATIBLE_MODIFIERS!>out<!>
    <!INCOMPATIBLE_MODIFIERS!>in<!>
    vararg
    reified
    const
    fun foo() {}
}

// Secondary constructors
class IllegalModifiers8 {
    <!INCOMPATIBLE_MODIFIERS!>abstract<!>
    enum
    <!INCOMPATIBLE_MODIFIERS, REDUNDANT_MODIFIER!>open<!>
    inner
    annotation
    <!INCOMPATIBLE_MODIFIERS!>override<!>
    <!INCOMPATIBLE_MODIFIERS!>out<!>
    <!INCOMPATIBLE_MODIFIERS!>in<!>
    <!INCOMPATIBLE_MODIFIERS!>final<!>
    vararg
    reified
    <!INCOMPATIBLE_MODIFIERS!>const<!><!SYNTAX!><!>
    constructor() {}

    constructor(<!INCOMPATIBLE_MODIFIERS!>private<!> enum <!INCOMPATIBLE_MODIFIERS!>abstract<!> x: Int) {}
}

class IllegalModifiers9 {
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>protected<!> constructor() {}
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>internal<!> constructor(x: Int) {}
}

// Illegal modifiers on primary constructor

class IllegalModifiers10
<!INCOMPATIBLE_MODIFIERS!>abstract<!>
enum
<!INCOMPATIBLE_MODIFIERS, REDUNDANT_MODIFIER!>open<!>
inner
annotation
<!INCOMPATIBLE_MODIFIERS!>override<!>
<!INCOMPATIBLE_MODIFIERS!>out<!>
<!INCOMPATIBLE_MODIFIERS!>in<!>
<!INCOMPATIBLE_MODIFIERS!>final<!>
vararg
reified
<!INCOMPATIBLE_MODIFIERS!>const<!> constructor()

class IllegalModifiers11 <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>protected<!> constructor()

class Outer {
    <!INCOMPATIBLE_MODIFIERS!>inner<!> <!INCOMPATIBLE_MODIFIERS!>sealed<!> class Inner
}
