import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(self: Any?, property: KProperty<*>): Int = 42
    operator fun setValue(self: Any?, property: KProperty<*>, value: Int) {
        println("Setting the value $value")
    }
}

class NormalDelegatedProperty {
    protected var p6 by Delegate()
}

open class A {
    // when a property has both a delegate and a
    // getter with a greater visibility, we get a
    // no applicable candidates message, because
    // the delegate's getter type differs from the
    // property's getter return type
    <!PROPERTY_WITH_DELEGATE_AND_EXPOSING_GETTER!>open protected var p1 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number<!>

    <!PROPERTY_WITH_DELEGATE_AND_EXPOSING_GETTER!>open protected var p2 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number<!>

    <!PROPERTY_WITH_DELEGATE_AND_EXPOSING_GETTER!>open protected var p3 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number<!>

    <!PROPERTY_WITH_DELEGATE_AND_EXPOSING_GETTER!>open protected var p4 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number<!>

    <!PROPERTY_WITH_DELEGATE_AND_EXPOSING_GETTER!>open protected var p5 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
        public get(): Number<!>

    <!PROPERTY_WITH_DELEGATE_AND_EXPOSING_GETTER!>open protected val p6 by Delegate()
        public get(): Number<!>

    <!PROPERTY_WITH_DELEGATE_AND_EXPOSING_GETTER!>open protected val p7 by Delegate()
        public get(): Number<!>

    <!PROPERTY_WITH_DELEGATE_AND_EXPOSING_GETTER!>open protected val p8 by Delegate()
        public get(): Number<!>
}

class B : A() {
    public override var p1 = super.p1 <!UNRESOLVED_REFERENCE!>*<!> 2

    override var p2 = super.p2 <!UNRESOLVED_REFERENCE!>*<!> 2
        public get(): Number

    <!MUST_BE_INITIALIZED!>public override var p3<!> get() = super.p3 <!UNRESOLVED_REFERENCE!>*<!> 2

    <!MUST_BE_INITIALIZED!>public override var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>p4<!><!> get() = 2

    <!MUST_BE_INITIALIZED!>public override var p5<!> get() = super.p5

    public override val p6 get() = super.p6 <!UNRESOLVED_REFERENCE!>*<!> 2

    public override val p7 get() = 2

    public override val p8 get() = super.p8
}
