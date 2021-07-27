open class A {
    protected open val items: MutableList<String> = mutableListOf("a", "b")
        public get(): Collection<String>
}

open class B : A() {
    public override val items: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>List<String><!> = listOf("a", "b")
}

class ConcreteA : A() {
    fun doSomething() {
        items.add("item")
    }
}

class ConcreteB : B() {
    fun doSomething() {
        items.<!UNRESOLVED_REFERENCE!>add<!>("item")
    }
}
