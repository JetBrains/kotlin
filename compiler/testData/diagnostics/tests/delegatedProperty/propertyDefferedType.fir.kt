// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

import kotlin.reflect.KProperty

class B {
    val c <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> <!CANNOT_INFER_PARAMETER_TYPE!>Delegate<!>(<!UNRESOLVED_REFERENCE!>ag<!>)
}

class Delegate<T: Any>(val init: T) {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = null!!
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, nullableType, operator,
primaryConstructor, propertyDeclaration, propertyDelegate, starProjection, typeConstraint, typeParameter */
