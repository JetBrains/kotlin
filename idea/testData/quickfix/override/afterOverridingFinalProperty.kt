// "Make A.x open" "true"
open class A {
    open var x = 42;
}

class B : A() {
    override<caret> var x = 24;
}