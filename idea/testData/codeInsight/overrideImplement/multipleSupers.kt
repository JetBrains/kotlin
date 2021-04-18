// FIR_IDENTICAL
open class A {
    open fun foo() {}
}

interface B {
    fun bar()
}

class C : A(), B {
   <caret>
}
