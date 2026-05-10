// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82684

import kotlin.reflect.KClass

annotation class ExpectInt(val x: Int = <!CANNOT_INFER_PARAMETER_TYPE!>arrayOf()<!>)
annotation class ExpectString(val x: String = <!CANNOT_INFER_PARAMETER_TYPE!>arrayOf()<!>)

enum class E { ENTRY }

annotation class ExpectEnum(val x: E = <!CANNOT_INFER_PARAMETER_TYPE!>arrayOf()<!>)
annotation class ExpectAnno(val x: ExpectInt = <!CANNOT_INFER_PARAMETER_TYPE!>arrayOf()<!>)
annotation class ExpectClass(val x: KClass<Int> = <!CANNOT_INFER_PARAMETER_TYPE!>arrayOf()<!>)

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, primaryConstructor,
propertyDeclaration */
