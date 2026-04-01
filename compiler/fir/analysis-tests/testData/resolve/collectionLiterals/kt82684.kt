// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82684

import kotlin.reflect.KClass

annotation class ExpectInt(val x: Int = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
annotation class ExpectString(val x: String = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)

enum class E { ENTRY }

annotation class ExpectEnum(val x: E = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
annotation class ExpectAnno(val x: ExpectInt = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
annotation class ExpectClass(val x: KClass<Int> = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, primaryConstructor,
propertyDeclaration */
