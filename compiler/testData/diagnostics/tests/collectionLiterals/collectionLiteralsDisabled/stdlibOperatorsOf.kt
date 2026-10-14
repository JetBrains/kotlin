// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CollectionLiterals +CompanionBlocks
// WITH_STDLIB

@file:OptIn(ExperimentalUnsignedTypes::class)

fun test() {
    val s0: Sequence<Int> = Sequence.<!OPT_IN_USAGE_ERROR!>of<!>()
    val s1 = Sequence.<!OPT_IN_USAGE_ERROR!>of<!>(42)
    val s3 = Sequence.<!OPT_IN_USAGE_ERROR!>of<!>('a', 'b', 'c')

    val ub0 = UByteArray.<!OPT_IN_USAGE_ERROR!>of<!>()
    val ubM = UByteArray.<!OPT_IN_USAGE_ERROR!>of<!>(1u, 2u)
    val us0 = UShortArray.<!OPT_IN_USAGE_ERROR!>of<!>()
    val usM = UShortArray.<!OPT_IN_USAGE_ERROR!>of<!>(1u, 2u)
    val ui0 = UIntArray.<!OPT_IN_USAGE_ERROR!>of<!>()
    val uiM = UIntArray.<!OPT_IN_USAGE_ERROR!>of<!>(1u, 2u)
    val ul0 = ULongArray.<!OPT_IN_USAGE_ERROR!>of<!>()
    val ulM = ULongArray.<!OPT_IN_USAGE_ERROR!>of<!>(1uL, 2uL)

    val b = ByteArray.<!OPT_IN_USAGE_ERROR!>of<!>(1, 2)
    val s = ShortArray.<!OPT_IN_USAGE_ERROR!>of<!>(1, 2)
    val i: IntArray = IntArray.<!OPT_IN_USAGE_ERROR!>of<!>()
    val l = LongArray.<!OPT_IN_USAGE_ERROR!>of<!>(1)
    val f = FloatArray.<!OPT_IN_USAGE_ERROR!>of<!>(1f)
    val d = DoubleArray.<!OPT_IN_USAGE_ERROR!>of<!>(1.0, 2.0)
    val c: CharArray = CharArray.<!OPT_IN_USAGE_ERROR!>of<!>()
    val z = BooleanArray.<!OPT_IN_USAGE_ERROR!>of<!>(true, false, true)

    val strings = Array.<!OPT_IN_USAGE_ERROR!>of<!>("a", "b", "c")
    val nullables = Array.<!OPT_IN_USAGE_ERROR!>of<!>(1, null, 3)
    val anys: Array<Any> = Array.<!OPT_IN_USAGE_ERROR!>of<!>(Any())
}

@OptIn(ExperimentalCollectionLiteralsApi::class)
fun testWithOptIn() {
    val s0: Sequence<Int> = Sequence.of()
    val s1 = Sequence.of(42)
    val s3 = Sequence.of('a', 'b', 'c')

    val ub0 = UByteArray.of()
    val ubM = UByteArray.of(1u, 2u)
    val us0 = UShortArray.of()
    val usM = UShortArray.of(1u, 2u)
    val ui0 = UIntArray.of()
    val uiM = UIntArray.of(1u, 2u)
    val ul0 = ULongArray.of()
    val ulM = ULongArray.of(1uL, 2uL)

    val b = ByteArray.of(1, 2)
    val s = ShortArray.of(1, 2)
    val i: IntArray = IntArray.of()
    val l = LongArray.of(1)
    val f = FloatArray.of(1f)
    val d = DoubleArray.of(1.0, 2.0)
    val c: CharArray = CharArray.of()
    val z = BooleanArray.of(true, false, true)

    val strings = Array.of("a", "b", "c")
    val nullables = Array.of(1, null, 3)
    val anys: Array<Any> = Array.of(Any())
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, unsignedLiteral */
