// !DIAGNOSTICS: -UNUSED_VALUE -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// !LANGUAGE: +LateinitLocalVariables

import kotlin.reflect.KProperty

object Delegate {
    operator fun getValue(instance: Any?, property: KProperty<*>) : String = ""
    operator fun setValue(instance: Any?, property: KProperty<*>, value: String) {}
}


fun test() {
    lateinit val test0: Any
    lateinit var test1: Int
    lateinit var test2: Any?
    lateinit var test3: String = ""
    lateinit var test4 by Delegate
}
