// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82684

import kotlin.reflect.KClass

annotation class ExpectInt(val x: Int = [])
annotation class ExpectString(val x: String = [])

enum class E { ENTRY }

annotation class ExpectEnum(val x: E = [])
annotation class ExpectAnno(val x: ExpectInt = [])
annotation class ExpectClass(val x: KClass<Int> = [])

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, primaryConstructor,
propertyDeclaration */
