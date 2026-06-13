// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE_FEATURE_TOGGLED: CollectionLiterals
// WITH_STDLIB

@file:OptIn(ExperimentalUnsignedTypes::class)

annotation class VarargIntAnno(vararg val v: UInt)
annotation class IntArrayAnno(val v: UIntArray)
annotation class LongArrayAnno(val v: ULongArray)

@VarargIntAnno(*uintArrayOf(*<!ARGUMENT_TYPE_MISMATCH!>[1u, 2u, 3u]<!>))
@IntArrayAnno(uintArrayOf(elements = <!ARGUMENT_TYPE_MISMATCH!>[1u, 2u, 3u]<!>))
@LongArrayAnno(v = ulongArrayOf(0u, *<!ARGUMENT_TYPE_MISMATCH!>[1u, 2u, 3u]<!>, 4u))
fun test() { }

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, classReference, functionDeclaration,
outProjection, primaryConstructor, propertyDeclaration, unsignedLiteral, vararg */
