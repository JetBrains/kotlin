import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(self: Any?, property: KProperty<*>): Int = 42
    operator fun setValue(self: Any?, property: KProperty<*>, value: Int) {
        println("Setting the value $value")
    }
}

open class A {
    open protected var p1 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number

    open protected var p2 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number

    open protected var p3 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number

    open protected var p4 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number

    open protected var p5 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number

    open protected val p6 by Delegate()
        public get(): Number

    open protected val p7 by Delegate()
        public get(): Number

    open protected val p8 by Delegate()
        public get(): Number
}

class B : A() {
    public override var p1 = super.p1 <!UNRESOLVED_REFERENCE!>*<!> 2

    override var p2 = super.p2 <!UNRESOLVED_REFERENCE!>*<!> 2
        public get(): Number

    <!MUST_BE_INITIALIZED!>public override var p3<!> get() = super.p3 <!UNRESOLVED_REFERENCE!>*<!> 2

    <!MUST_BE_INITIALIZED!>public override var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>p4<!><!> get() = 2

    <!MUST_BE_INITIALIZED!>public override var p5<!> get() = super.p5

    // without a delegate this resolves into
    // and Int and the multiplication works
    // but here we see UNRESOLVED_REFERENCE
    public override val p6 get() = super.p6 * 2

    public override val p7 get() = 2

    public override val p8 get() = super.p8
}
