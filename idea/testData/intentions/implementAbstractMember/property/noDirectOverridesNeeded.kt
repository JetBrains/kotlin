// IS_APPLICABLE: false
// ERROR: Class 'C' is not abstract and does not implement abstract base class member public abstract val foo: Int defined in B
interface A {
    val <caret>foo: Int
}

class X : A {
    override val foo = 1
}

abstract class B : A {
    abstract override val foo: Int
}

class C: B() {

}