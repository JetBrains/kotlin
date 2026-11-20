// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

// MODULE: anno
// FILE: Anno.kt
@Repeatable
annotation class Anno(
    val int: IntArray,
    val char: CharArray,
    val double: DoubleArray,
    vararg val boolean: Boolean,
)

// MODULE: companionsOn(anno)
// LANGUAGE: +CompanionBlocks
// FILE: withCompanions.kt
@Anno(
    IntArray.of(),
    char = [],
    double = [42.0],
    *BooleanArray.of(true, false, true),
)
fun testOn() {
    val a: ByteArray = ByteArray.of(1, 2)
    val b: ShortArray = ShortArray.of()
    val c: IntArray = IntArray.of(1)
    val d: LongArray = LongArray.of(1, 2)
    val e: FloatArray = FloatArray.of(1f)
    val f: DoubleArray = DoubleArray.of(1.0, 2.0, 3.0)
    val g: CharArray = CharArray.of()
    val h: BooleanArray = BooleanArray.of(true, false, true)

    val ab: ByteArray = [1, 2, 3]
    val bb: ShortArray = [1]
    val cb: IntArray = [1]
    val db: LongArray = []
    val eb: FloatArray = []
    val fb: DoubleArray = [1.0]
    val gb: CharArray = []
    val hb: BooleanArray = []
}

// MODULE: companionsOff(anno)
// LANGUAGE: -CompanionBlocks
// FILE: withoutCompanions.kt
@Anno(
    [1, 2, 3],
    char = CharArray.<!UNSUPPORTED_FEATURE!>of<!>('a'),
    double = DoubleArray.<!UNSUPPORTED_FEATURE!>of<!>(),
    boolean = [],
)
fun testOff() {
    val a: ByteArray = ByteArray.<!UNSUPPORTED_FEATURE!>of<!>(1, 2)
    val b: ShortArray = ShortArray.<!UNSUPPORTED_FEATURE!>of<!>()
    val c: IntArray = IntArray.<!UNSUPPORTED_FEATURE!>of<!>(1)
    val d: LongArray = LongArray.<!UNSUPPORTED_FEATURE!>of<!>(1, 2)
    val e: FloatArray = FloatArray.<!UNSUPPORTED_FEATURE!>of<!>(1f)
    val f: DoubleArray = DoubleArray.<!UNSUPPORTED_FEATURE!>of<!>(1.0, 2.0, 3.0)
    val g: CharArray = CharArray.<!UNSUPPORTED_FEATURE!>of<!>()
    val h: BooleanArray = BooleanArray.<!UNSUPPORTED_FEATURE!>of<!>(true, false, true)

    val ab: ByteArray = [1, 2, 3]
    val bb: ShortArray = [1]
    val cb: IntArray = [1]
    val db: LongArray = []
    val eb: FloatArray = []
    val fb: DoubleArray = [1.0]
    val gb: CharArray = []
    val hb: BooleanArray = []
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral, localProperty, primaryConstructor,
propertyDeclaration, vararg */
