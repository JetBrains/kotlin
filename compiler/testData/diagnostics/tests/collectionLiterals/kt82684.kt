// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82684
// LANGUAGE: -CollectionLiterals
// LANGUAGE_FEATURE_TOGGLED: CollectionLiteralsBasedAnnotationResolution

import kotlin.reflect.KClass

annotation class ExpectInt(val x: Int = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_COLLECTION_LITERAL, UNSUPPORTED_FEATURE!>[]<!>)
annotation class ExpectString(val x: String = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_COLLECTION_LITERAL, UNSUPPORTED_FEATURE!>[]<!>)

enum class E { ENTRY }

annotation class ExpectEnum(val x: E = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_COLLECTION_LITERAL, UNSUPPORTED_FEATURE!>[]<!>)
annotation class ExpectAnno(val x: ExpectInt = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_COLLECTION_LITERAL, UNSUPPORTED_FEATURE!>[]<!>)
annotation class ExpectClass(val x: KClass<Int> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, UNRESOLVED_COLLECTION_LITERAL, UNSUPPORTED_FEATURE!>[]<!>)

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, primaryConstructor,
propertyDeclaration */
