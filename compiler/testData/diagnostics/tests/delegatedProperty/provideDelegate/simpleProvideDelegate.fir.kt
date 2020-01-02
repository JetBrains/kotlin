// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

operator fun String.provideDelegate(a: Any?, p: KProperty<*>) = this
operator fun String.getValue(a: Any?, p: KProperty<*>) = this

val test1: String by "OK"

val test2 by "OK"