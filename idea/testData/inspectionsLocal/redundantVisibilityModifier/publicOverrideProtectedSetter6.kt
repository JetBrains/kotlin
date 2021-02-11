abstract class A {
    open var attribute = "a"
        protected set
}

abstract class B : A() {
    override var attribute = "b"
        public set
}

class C : B() {
    <caret>public override var attribute = super.attribute
}

fun main() {
    val c = C()
    c.attribute = "test"
}

// IGNORE_FIR KT-44939