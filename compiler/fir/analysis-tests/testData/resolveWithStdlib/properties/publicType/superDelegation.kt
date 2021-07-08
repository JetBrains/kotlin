open class A {
    open protected var p1 = 10
        public get(): Number

    open protected var p2 = 10
        public get(): Number

    open protected var p3 = 10
        public get(): Number

    open protected var p4 = 10
        public get(): Number

    open protected var p5 = 10
        public get(): Number

    open protected val p6 = 10
        public get(): Number

    open protected val p7 = 10
        public get(): Number

    open protected val p8 = 10
        public get(): Number
}

class B : A() {
    public override var p1 = super.p1 * 2

    override var p2 = super.p2 * 2
        public get(): Number

    <!MUST_BE_INITIALIZED!>public override var p3<!> get() = super.p3 * 2

    <!MUST_BE_INITIALIZED!>public override var p4<!> get() = 2

    <!MUST_BE_INITIALIZED!>public override var p5<!> get() = super.p5

    public override val p6 get() = super.p6 * 2

    public override val p7 get() = 2

    public override val p8 get() = super.p8
}
