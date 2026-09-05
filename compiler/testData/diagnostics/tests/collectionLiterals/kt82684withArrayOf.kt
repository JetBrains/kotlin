// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82684
// LANGUAGE_FEATURE_TOGGLED: CollectionLiteralsBasedAnnotationResolution

import kotlin.reflect.KClass

annotation class ExpectInt(val x: Int = <!CANNOT_INFER_PARAMETER_TYPE, INITIALIZER_TYPE_MISMATCH!>arrayOf<!>())
annotation class ExpectString(val x: String = <!CANNOT_INFER_PARAMETER_TYPE, INITIALIZER_TYPE_MISMATCH!>arrayOf<!>())

enum class E { ENTRY }

annotation class ExpectEnum(val x: E = <!CANNOT_INFER_PARAMETER_TYPE, INITIALIZER_TYPE_MISMATCH!>arrayOf<!>())
annotation class ExpectAnno(val x: ExpectInt = <!CANNOT_INFER_PARAMETER_TYPE, INITIALIZER_TYPE_MISMATCH!>arrayOf<!>())
annotation class ExpectClass(val x: KClass<Int> = <!CANNOT_INFER_PARAMETER_TYPE, INITIALIZER_TYPE_MISMATCH!>arrayOf<!>())

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, primaryConstructor,
propertyDeclaration */
