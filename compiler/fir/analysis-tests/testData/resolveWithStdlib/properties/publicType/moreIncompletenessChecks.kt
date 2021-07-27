open class A {
    open protected val names = mutableListOf("a", "b")
        public get(): Any

    open protected var tags = mutableListOf("a", "b")
        public get(): Any
}

class B : A() {
    <!INCOMPLETE_PROPERTY_OVERRIDE!>override val names = mutableListOf("a", "b")<!>
    <!INCOMPLETE_PROPERTY_OVERRIDE!>override var tags = mutableListOf("a", "b")<!>
}
