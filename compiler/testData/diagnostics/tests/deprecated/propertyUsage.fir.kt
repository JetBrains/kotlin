// !DIAGNOSTICS: -UNUSED_EXPRESSION

import kotlin.reflect.KProperty

class Delegate() {
    @Deprecated("text")
    operator fun getValue(instance: Any, property: KProperty<*>) : Int = 1

    @Deprecated("text")
    operator fun setValue(instance: Any, property: KProperty<*>, value: Int) {}
}

class Delegate2() {
    operator fun getValue(instance: Any, property: KProperty<*>) : Int = 1
    operator fun setValue(instance: Any, property: KProperty<*>, value: Int) {}
}

class DelegateProvider() {
    operator fun provideDelegate(instance: Any, property: KProperty<*>) = Delegate2()
}

class PropertyHolder {
    @Deprecated("text")
    val x = 1

    @Deprecated("text")
    var name = "String"

    val valDelegate by <!DEPRECATION!>Delegate<!>()
    var varDelegate by <!DEPRECATION, DEPRECATION!>Delegate<!>()

    // no deprecation caused by access to itself
    @Deprecated("text")
    var deprecatedDelegated by Delegate2()

    // no deprecation caused by access to itself
    @Deprecated("text")
    val deprecatedDelegated2 by DelegateProvider()

    public val test1: String = ""
        @Deprecated("val-getter") get

    public var test2: String = ""
        @Deprecated("var-getter") get
        @Deprecated("var-setter") set

    public var test3: String = ""
        @Deprecated("var-getter") get
        set

    public var test4: String = ""
        get
        @Deprecated("var-setter") set
}

fun PropertyHolder.extFunction() {
    <!DEPRECATION!>test2<!> = "ext"
    <!DEPRECATION!>test1<!>
}

fun fn() {
    PropertyHolder().<!DEPRECATION!>test1<!>
    PropertyHolder().<!DEPRECATION!>test2<!>
    PropertyHolder().<!DEPRECATION!>test2<!> = ""

    PropertyHolder().<!DEPRECATION!>test3<!>
    PropertyHolder().test3 = ""

    PropertyHolder().test4
    PropertyHolder().<!DEPRECATION!>test4<!> = ""

    val a = PropertyHolder().<!DEPRECATION!>x<!>
    val b = PropertyHolder().<!DEPRECATION!>name<!>
    PropertyHolder().<!DEPRECATION!>name<!> = "value"

    val d = PropertyHolder().valDelegate
    PropertyHolder().varDelegate = 1
}

fun literals() {
    PropertyHolder::test1
    PropertyHolder::<!DEPRECATION!>name<!>
}
