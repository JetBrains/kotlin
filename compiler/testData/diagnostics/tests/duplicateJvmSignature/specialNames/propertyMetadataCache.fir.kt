// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

operator fun Any.getValue(x: Any?, y: Any): Any = null!!

class C {
    val x by <!UNRESOLVED_REFERENCE!>1<!>
    val `$$delegatedProperties`: Array<KProperty<*>> = null!!
}

val x by <!UNRESOLVED_REFERENCE!>1<!>
val `$$delegatedProperties`: Array<KProperty<*>> = null!!
