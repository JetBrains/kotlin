open class A {
    open protected val items = listOf<String>()
        public get(): Any
}

class B : A() {
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>private<!> override val items = mutableListOf<String>()
        public get(): Collection<String>
}

class C : A() {
    protected override val items = mutableListOf<String>()
        public get(): Collection<String>
}

abstract class Fooer {
    abstract val foo: Number
    abstract var bar: Number
    open val baz: Number = 3.14
}

class SimpleFooer : Fooer() {
    protected override val foo: Int = 2
        public get(): Number

    protected override var bar: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Int<!> = 2
        public get(): Number

    protected override val baz: Int = 2
        public get(): Number
}
