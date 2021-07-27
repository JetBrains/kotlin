// FIR_IDENTICAL
open class A {
    private open val number = 4
        public get(): Number
}

class B : A() {
    public override val number = 10
}

class C : A() {
    public override val number get() = 10
}

class D : A() {
    override val number = 10
        public get(): Number

    public val count = 1
        private set
}

class E : A() {
    protected override val number = 10
        public get(): Number
}

class F : A() {
    private override val number = 20
        public get(): Number
}
