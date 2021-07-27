open class A {
    open protected var p1 = 10
        public get(): Number

    open protected var p2 = 10
        public get(): Any

    open protected var p3 = 10
        public get(): Any

    private open var p4 = 10
        public get(): Number

    private open var p5 = 10
        public get(): Number
}

class B : A() {
    // this must be an error
    override var p1 = super.p1 * 2
        public get(): <!PROPERTY_GETTER_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>

    override var p2 = super.p2 * 2
        public get(): Number

    // error must be here
    public override var p3: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Number<!> = 20

    protected override var p4 = 10
        public get(): Number

    // p5 inheirts its private visibility
    override var p5 = 20
        public get(): Number
}
