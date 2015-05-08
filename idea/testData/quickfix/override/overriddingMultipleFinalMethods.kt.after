// "Make 'foo' in A, X and Y open" "true"
open class A {
    open fun foo() {}
}

trait X {
    open fun foo() {}
}

trait Y {
    open fun foo() {}
}

trait Z {
    fun foo() {}
}

class B : A(), X, Y, Z {
    override<caret> fun foo() {}
}
