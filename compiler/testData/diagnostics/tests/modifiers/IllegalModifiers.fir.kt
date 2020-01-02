@<!SYNTAX!><!>myAnnotation public
package illegal_modifiers

abstract class A() {
    abstract final fun f()
    abstract open fun g()
    final open fun h() {}

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

private public class C
private public object D

//A sample annotation to check annotation usage in parameters.
annotation class annotated(val text: String = "not given")

//Check legal modifiers in constructor
class LegalModifier(val a: Int, @annotated private var b: String, @annotated vararg v: Int)

//Check illegal modifier in constructor parameters
class IllegalModifiers1(
        in
        out
        reified
        enum
        private
        const
        a: Int)

//Check multiple illegal modifiers in constructor
class IllegalModifiers2(private abstract a: Int)


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
    try {} catch (in out reified enum public e: Exception) {}

    //Check multiple illegal modifiers in catch block
    try {} catch (in out reified enum abstract public e: Exception) {}

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
out
in
vararg
reified
class IllegalModifiers7() {
    enum
    inner
    annotation
    out
    in
    vararg
    reified
    val x = 1
    enum
    inner
    annotation
    out
    in
    vararg
    reified
    const
    fun foo() {}
}

// Secondary constructors
class IllegalModifiers8 {
    abstract
    enum
    open
    inner
    annotation
    override
    out
    in
    final
    vararg
    reified
    const<!SYNTAX!><!>
    constructor() {}

    constructor(private enum abstract x: Int) {}
}

class IllegalModifiers9 {
    private protected constructor() {}
    private internal constructor(x: Int) {}
}

// Illegal modifiers on primary constructor

class IllegalModifiers10
abstract
enum
open
inner
annotation
override
out
in
final
vararg
reified
const constructor()

class IllegalModifiers11 private protected constructor()

class Outer {
    inner sealed class Inner
}
