// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE_FEATURE_TOGGLED: CollectionLiterals

annotation class VarargIntAnno(vararg val v: Int)
annotation class IntArrayAnno(val v: IntArray)
annotation class LongArrayAnno(val v: LongArray)
annotation class CharArrayAnno(val v: CharArray)
annotation class BooleanArrayAnno(val v: BooleanArray)

@VarargIntAnno(*intArrayOf(*[1, 2, 3]))
@IntArrayAnno(intArrayOf(elements = [1, 2, 3]))
@LongArrayAnno(v = longArrayOf(0, *[1, 2, 3], 4))
@CharArrayAnno(charArrayOf(*[]))
@BooleanArrayAnno(v = booleanArrayOf(elements = [true, false]))
fun test() { }

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration, vararg */
