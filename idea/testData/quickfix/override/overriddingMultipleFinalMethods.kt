// "Make 'foo' in A, X and Y open" "true"
open class A {
    fun foo() {}
}

interface X {
    final fun foo() {}
}

interface Y {
    final fun foo() {}
}

interface Z {
    fun foo() {}
}

class B : A(), X, Y, Z {
    override<caret> fun foo() {}
}
/* IGNORE_FIR */
