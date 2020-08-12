// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

interface A {
    val prop: Int
}

class AImpl: A  {
    override val prop by Delegate()
}

fun foo() {
    AImpl().prop
}

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>String<!> {
        return ""
    }
}
