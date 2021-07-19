// WITH_RUNTIME

abstract class A {
    abstract val tags1: List<String>

    private abstract val tags2: List<String>
        public get(): Collection<String>

    protected abstract val tags3: List<String>
        public get(): Collection<String>

    private abstract val tags4: List<String>
        protected get(): Collection<String>
}

open class B : A() {
    private override val tags1 = mutableListOf("a", "b")
        public get(): List<String>

    protected override val tags2 = mutableListOf("c", "d")
        public get(): List<String>

    public override val tags3 = mutableListOf("e", "f")

    protected override val tags4 = mutableListOf("g", "h")
}

class C : B() {
    fun fill() {
        tags1.<!UNRESOLVED_REFERENCE!>add<!>("[x]")
        tags2.add("[x]")
        tags3.add("[y]")
        tags4.add("[z]")
    }
}
