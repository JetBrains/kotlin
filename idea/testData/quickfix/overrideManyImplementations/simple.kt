// "Override 'foo'" "true"
interface A {
    fun foo() {}
}

open class B {
    open fun foo() {}
}

class<caret> C : A, B()