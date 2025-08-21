// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A<R>() {
    operator fun <T> getValue(t: Any?, p: KProperty<*>): T = null!!
    operator fun <T> setValue(t: Any?, p: KProperty<*>, x: T) = Unit
}

var a1: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> <!CANNOT_INFER_PARAMETER_TYPE!>A<!>()
var a2: Int by A<String>()

class B<R>() {
    operator fun <T> getValue(t: Any?, p: KProperty<*>): T = null!!
    operator fun setValue(t: Any?, p: KProperty<*>, x: R) = Unit
}

var b1: Int by B()
var b2: Int by B<Number>()

class C<R>() {
    operator fun getValue(t: Any?, p: KProperty<*>): R = null!!
    operator fun <T> setValue(t: Any?, p: KProperty<*>, x: T) = Unit
}

var c1: Int by C()
var c2: Int <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>by<!> C<Number>()

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, nullableType, operator,
primaryConstructor, propertyDeclaration, propertyDelegate, setter, starProjection, typeParameter */
