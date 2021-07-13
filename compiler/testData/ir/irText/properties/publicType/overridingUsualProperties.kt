// FIR_IDENTICAL
open class A {
    open protected val number: Number = 3
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
}

class E : A() {
    private override val number = 10
        public get(): Number
}
