// RUN_PIPELINE_TILL: FRONTEND

import kotlin.reflect.KProperty1

class WithTypeParameter<T> {
    val value: T?
        field : T = null!!

    fun callableReference() {
        val x = WithTypeParameter<String>()
        val y: String <!INITIALIZER_TYPE_MISMATCH!>=<!> x::value.get()
        val z: KProperty1<WithTypeParameter<String>, String> <!INITIALIZER_TYPE_MISMATCH!>=<!> WithTypeParameter<String>::value
    }
}

/* GENERATED_FIR_TAGS: callableReference, checkNotNullCall, classDeclaration, explicitBackingField, functionDeclaration,
localProperty, nullableType, propertyDeclaration, typeParameter */
