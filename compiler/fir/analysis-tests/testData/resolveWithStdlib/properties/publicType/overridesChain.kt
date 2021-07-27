open class A {
    open protected val items = mutableListOf("a", "b")
        public get(): Any
}

open class B : A() {
    protected override val items = mutableListOf("a", "b")
        public get(): List<String>
}

class C : B() {
    protected override val items = mutableListOf("a", "b")
        public get(): <!PROPERTY_GETTER_TYPE_MISMATCH_ON_OVERRIDE!>Collection<String><!>
}

open class D : A() {
    override val items = mutableListOf("a", "b")
        public get(): List<String>
}

class E : D() {
    override val items = mutableListOf("a", "b")
        public get(): <!PROPERTY_GETTER_TYPE_MISMATCH_ON_OVERRIDE!>Collection<String><!>
}
