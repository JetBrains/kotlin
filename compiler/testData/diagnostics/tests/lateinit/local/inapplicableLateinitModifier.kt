// !DIAGNOSTICS: -UNUSED_VALUE -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// !LANGUAGE: +LateinitLocalVariables

import kotlin.reflect.KProperty

object Delegate {
    operator fun getValue(instance: Any?, property: KProperty<*>) : String = ""
    operator fun setValue(instance: Any?, property: KProperty<*>, value: String) {}
}


fun test() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val test0: Any
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var test1: Int
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var test2: Any?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var test3: String = ""
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var test4 by Delegate
}
