open class A {
    open val items: Collection<String> = listOf("a", "b")
}

open class B : A() {
    private override val items = listOf("a", "b")
        public get(): Collection<String>
}

open class C : B() {
    private override val items = listOf("a", "b")
        public get(): Collection<String>
}

open class D : C() {
    public override val items = listOf("a", "b")
}

open class E : D() {
    private override val items = mutableListOf("a", "b")
        public get(): List<String>
}

open class F : E() {
    protected override val items = mutableListOf("a", "b")
        public get(): List<String>
}

open class G : F() {
    public override val items = mutableListOf("a", "b")
}
