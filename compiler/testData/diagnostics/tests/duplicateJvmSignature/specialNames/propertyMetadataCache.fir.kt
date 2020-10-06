// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

operator fun Any.getValue(x: Any?, y: Any): Any = null!!

class C {
    val x by 1
    val `$$delegatedProperties`: Array<KProperty<*>> = null!!
}

val x by 1
val `$$delegatedProperties`: Array<KProperty<*>> = null!!
