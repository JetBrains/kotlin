open class Parent {
    override fun equals(other: Any?): Boolean =
        super.equals(other)
}
open class OperatorParent {
    override operator fun equals(other: Any?): Boolean =
        super.equals(other)
}

class A : Parent() {
    override fun equals(other: Any?): Boolean =
        super.equals(other)
}
class B : OperatorParent() {
    override fun equals(other: Any?): Boolean =
        super.equals(other)
}
class C : Parent() {
    override operator fun equals(other: Any?): Boolean = // false positive in K1, OK in K2
        super.equals(other) //
}
class D : OperatorParent() {
    override operator fun equals(other: Any?): Boolean = // false positive in K1, OK in K2
        super.equals(other)
}
