// IS_APPLICABLE: false
open class A {
    open operator fun plus(a: A) = A()
}

class B : A() {
    override fun plu<caret>s(a: A) = A()
}
