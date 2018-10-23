// "Change to var" "true"
open class A {
    open var x = 42;
}

class B : A() {
    override val<caret> x: Int = 3;
}