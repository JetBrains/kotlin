// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE_FEATURE_TOGGLED: CollectionLiterals

enum class E { X }
@Repeatable
annotation class EnumAnno(val v: Array<E>)
@Repeatable
annotation class VarargIntAnno(vararg val v: Int)

@EnumAnno(arrayOf(*arrayOf(*arrayOf(*[]))))
@EnumAnno(arrayOf(elements = []))
@VarargIntAnno(*intArrayOf(*intArrayOf(*[])))
@VarargIntAnno(42, *intArrayOf(elements = intArrayOf(elements = intArrayOf(elements = []))))
fun test() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, functionDeclaration,
integerLiteral, primaryConstructor, propertyDeclaration, vararg */
