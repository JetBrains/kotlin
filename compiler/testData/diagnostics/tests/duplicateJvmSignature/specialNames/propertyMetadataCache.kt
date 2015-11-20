// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

operator fun Any.getValue(x: Any?, y: Any): Any = null!!

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> {
    val x by 1
    <!CONFLICTING_JVM_DECLARATIONS!>val `$$delegatedProperties`: Array<KProperty<*>><!> = null!!
}

val x by 1
<!CONFLICTING_JVM_DECLARATIONS!>val `$$delegatedProperties`: Array<KProperty<*>><!> = null!!
