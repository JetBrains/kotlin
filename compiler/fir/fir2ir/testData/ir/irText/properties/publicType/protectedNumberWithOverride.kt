// FIR_IDENTICAL
open class A {
    open protected val number = 4
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
}
