// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_IR

import kotlin.reflect.KProperty

operator fun Any.getValue(x: Any?, y: Any): Any = null!!

class C {
    val x by 1
    val `$$delegatedProperties`: Array<KProperty<*>> = null!!
}

class C2 {
    val x by 1
    lateinit var `$$delegatedProperties`: Array<KProperty<*>>
}

val x by 1
lateinit var `$$delegatedProperties`: Array<KProperty<*>>