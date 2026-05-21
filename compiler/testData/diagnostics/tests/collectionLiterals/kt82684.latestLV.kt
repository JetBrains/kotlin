// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82684
// LATEST_LV_DIFFERENCE

import kotlin.reflect.KClass

annotation class ExpectInt(val x: Int = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_REFERENCE!>[]<!>)
annotation class ExpectString(val x: String = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_REFERENCE!>[]<!>)

enum class E { ENTRY }

annotation class ExpectEnum(val x: E = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_REFERENCE!>[]<!>)
annotation class ExpectAnno(val x: ExpectInt = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_REFERENCE!>[]<!>)
annotation class ExpectClass(val x: KClass<Int> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_REFERENCE!>[]<!>)

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, primaryConstructor,
propertyDeclaration */
