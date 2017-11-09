// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class StringDelegate(val s: String) {
    operator fun getValue(a: Any?, p: KProperty<*>): Int = 42
}

// NB no operator
fun String.provideDelegate(a: Any?, p: KProperty<*>) = StringDelegate(this)

operator fun String.getValue(a: Any?, p: KProperty<*>) = this

val test1: String by "OK"
val test2: Int by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>"OK"<!>
val test3 by "OK"

