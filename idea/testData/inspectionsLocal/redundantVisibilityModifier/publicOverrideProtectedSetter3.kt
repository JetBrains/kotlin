abstract class A {
    open var attribute = "a"
        protected set
}

class C : A() {
    <caret>public override var attribute = super.attribute
        public set
}

fun main() {
    val c = C()
    c.attribute = "test"
}
