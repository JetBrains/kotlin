// "Make 'foo' in A, X and Y open" "true"
open class A {
    fun foo() {}
}

trait X {
    final fun foo() {}
}

trait Y {
    final fun foo() {}
}

trait Z {
    fun foo() {}
}

class B : A(), X, Y, Z {
    override<caret> fun foo() {}
}
