// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82684

import kotlin.reflect.KClass

annotation class ExpectInt(val x: Int = arrayOf())
annotation class ExpectString(val x: String = arrayOf())

enum class E { ENTRY }

annotation class ExpectEnum(val x: E = arrayOf())
annotation class ExpectAnno(val x: ExpectInt = arrayOf())
annotation class ExpectClass(val x: KClass<Int> = arrayOf())

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, primaryConstructor,
propertyDeclaration */
