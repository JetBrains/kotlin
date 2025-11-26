// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82684

import kotlin.reflect.KClass

annotation class ExpectInt(val x: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> [])
annotation class ExpectString(val x: String <!INITIALIZER_TYPE_MISMATCH!>=<!> [])

enum class E { ENTRY }

annotation class ExpectEnum(val x: E <!INITIALIZER_TYPE_MISMATCH!>=<!> [])
annotation class ExpectAnno(val x: ExpectInt <!INITIALIZER_TYPE_MISMATCH!>=<!> [])
annotation class ExpectClass(val x: KClass<Int> <!INITIALIZER_TYPE_MISMATCH!>=<!> [])

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, primaryConstructor,
propertyDeclaration */
