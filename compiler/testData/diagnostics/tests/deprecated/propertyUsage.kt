// !DIAGNOSTICS: -UNUSED_EXPRESSION

import kotlin.reflect.KProperty

class Delegate() {
    @Deprecated("text")
    operator fun getValue(instance: Any, property: KProperty<*>) : Int = 1

    @Deprecated("text")
    operator fun setValue(instance: Any, property: KProperty<*>, value: Int) {}
}

class PropertyHolder {
    @Deprecated("text")
    val x = 1

    @Deprecated("text")
    var name = "String"

    val valDelegate by <!DEPRECATION!>Delegate()<!>
    var varDelegate by <!DEPRECATION, DEPRECATION!>Delegate()<!>

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

    val <!UNUSED_VARIABLE!>a<!> = PropertyHolder().<!DEPRECATION!>x<!>
    val <!UNUSED_VARIABLE!>b<!> = PropertyHolder().<!DEPRECATION!>name<!>
    PropertyHolder().<!DEPRECATION!>name<!> = "value"

    val <!UNUSED_VARIABLE!>d<!> = PropertyHolder().valDelegate
    PropertyHolder().varDelegate = 1
}

fun literals() {
    PropertyHolder::test1
    PropertyHolder::<!DEPRECATION!>name<!>
}
