open class A {
    open fun foo() {}
}

trait B {
    fun bar()
}

class C : A(), B {
   <caret>
}
