// RUN_PIPELINE_TILL: FRONTEND

enum class E { X }

val x: Array<E> = arrayOf(E.X)

annotation class VarargEnumAnno(vararg val e: E = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>x<!>)
annotation class ArrayEnumAnno(val e: Array<E> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>arrayOf(*x)<!>)
annotation class ArrayEnumAnnoNamed(val e: Array<E> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>arrayOf(elements = x)<!>)

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, outProjection,
primaryConstructor, propertyDeclaration, vararg */
