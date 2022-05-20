import kotlin.reflect.KProperty

class MyDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = "Test"
}

val a by <!WRONG_MODIFIER_TARGET!>inline<!> MyDelegate()
val b by <!WRONG_MODIFIER_TARGET!>lateinit<!> MyDelegate()
