// "Change to var" "true"
open class A {
    open var x = 42;
}

class B(override val<caret> x: Int) : A()