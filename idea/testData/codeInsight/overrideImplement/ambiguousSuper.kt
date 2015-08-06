interface I {
    open fun foo(){}
}

open class A {
    open fun foo(){}
}

class C : A(), I {
    <caret>
}