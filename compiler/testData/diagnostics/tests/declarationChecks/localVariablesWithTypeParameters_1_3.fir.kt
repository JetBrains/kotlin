// !DIAGNOSTICS: -UNUSED_VARIABLE
// !LANGUAGE: -ProhibitTypeParametersForLocalVariables

import kotlin.reflect.KProperty

fun test() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING!><T><!> a0 = 0
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING!><T : <!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!>><!> a1 = ""
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING!><T : <!FINAL_UPPER_BOUND!>String<!>><!> a2 = 0
    <!WRONG_MODIFIER_TARGET!>const<!> val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING!><T><!> a3 = 0
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING!><T><!> a4 = 0
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING!><T><!> a5 by Delegate<Int>()
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING!><T><!> a6 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate<<!UNRESOLVED_REFERENCE!>T<!>>()<!>
}

class Delegate<F> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {}
}
