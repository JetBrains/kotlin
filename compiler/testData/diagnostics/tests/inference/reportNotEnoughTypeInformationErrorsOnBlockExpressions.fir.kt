// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// SKIP_TXT
// DIAGNOSTICS: -CAST_NEVER_SUCCEEDS

import kotlin.experimental.ExperimentalTypeInference

class Foo7<T>

fun foo7() = null as Foo7<Int>

fun poll17(flag: Boolean): Any? {
    val inv = if (flag) { foo7() } else { ::<!CANNOT_INFER_PARAMETER_TYPE!>Foo7<!> }
    return inv
}

fun poll26(flag: Boolean): Any? {
    val inv = when (flag) { true -> ::<!CANNOT_INFER_PARAMETER_TYPE!>Foo7<!> false -> foo7() <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ::<!CANNOT_INFER_PARAMETER_TYPE!>Foo7<!> }
    return inv
}

fun poll36(flag: Boolean): Any? {
    val inv = when (flag) { true -> ::<!CANNOT_INFER_PARAMETER_TYPE!>Foo7<!> false -> foo7() }
    return inv
}

fun poll56(): Any? {
    val inv = try { ::<!CANNOT_INFER_PARAMETER_TYPE!>Foo7<!> } catch (e: Exception) { foo7() } finally { foo7() }
    return inv
}

/* GENERATED_FIR_TAGS: asExpression, callableReference, classDeclaration, equalityExpression, functionDeclaration,
ifExpression, localProperty, nullableType, propertyDeclaration, smartcast, tryExpression, typeParameter, whenExpression,
whenWithSubject */
