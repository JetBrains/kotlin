// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// LANGUAGE: +ProhibitTypeParametersForLocalVariables

import kotlin.reflect.KProperty

fun test() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> a0 = 0
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T : <!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!>><!> a1 = ""
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T : <!FINAL_UPPER_BOUND!>String<!>><!> a2 = 0
    <!WRONG_MODIFIER_TARGET!>const<!> val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> a3 = 0
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> a4 = 0
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> a5 by Delegate<Int>()
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> a6 <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate<<!UNRESOLVED_REFERENCE!>T<!>>()
}

class Delegate<F> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {}
}
