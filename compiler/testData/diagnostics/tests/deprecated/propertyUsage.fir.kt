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

    val valDelegate by Delegate()
    var varDelegate by Delegate()

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
    test2 = "ext"
    test1
}

fun fn() {
    PropertyHolder().test1
    PropertyHolder().test2
    PropertyHolder().test2 = ""

    PropertyHolder().test3
    PropertyHolder().test3 = ""

    PropertyHolder().test4
    PropertyHolder().test4 = ""

    val a = PropertyHolder().x
    val b = PropertyHolder().name
    PropertyHolder().name = "value"

    val d = PropertyHolder().valDelegate
    PropertyHolder().varDelegate = 1
}

fun literals() {
    PropertyHolder::test1
    PropertyHolder::name
}
