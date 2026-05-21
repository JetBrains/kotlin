// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE_FEATURE_TOGGLED: CollectionLiterals

enum class E { X }
@Repeatable
annotation class EnumAnno(val v: Array<E>)
@Repeatable
annotation class VarargIntAnno(vararg val v: Int)

@EnumAnno(<!ARGUMENT_TYPE_MISMATCH!>arrayOf(*arrayOf(*arrayOf(*[])))<!>)
@EnumAnno(arrayOf(elements = []))
@VarargIntAnno(*intArrayOf(*intArrayOf(*<!ARGUMENT_TYPE_MISMATCH!>[]<!>)))
@VarargIntAnno(42, *intArrayOf(elements = intArrayOf(elements = intArrayOf(elements = <!ARGUMENT_TYPE_MISMATCH!>[]<!>))))
fun test() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, functionDeclaration,
integerLiteral, primaryConstructor, propertyDeclaration, vararg */
