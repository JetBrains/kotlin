// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTICS_FULL_TEXT

@OptIn(ExperimentalUnsignedTypes::class)
fun test() {
    val a: List<Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, null, 3]<!>
    val b: MutableList<Any> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[null]<!>
    val c: Set<String> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1]<!>
    val d: MutableSet<String> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["" as CharSequence]<!>
    val e: Sequence<Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1L, 2L, 3L]<!>

    val f: Array<Array<Int>> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>]<!>]<!>
    val g: IntArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>]<!>
    val h: LongArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[0.0]<!>
    val i: ShortArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1_000_000_000]<!>
    val j: ByteArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[if (true) 42.toByte() else 42]<!>
    val k: BooleanArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[Unit]<!>
    val l: FloatArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[0.0f as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Float<!>]<!>
    val m: DoubleArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42, 42.0]<!>

    val n: UIntArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>
    val o: ULongArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42L]<!>
    val p: UShortArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42.toShort()]<!>
    val q: UByteArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[{}]<!>
}

/* GENERATED_FIR_TAGS: asExpression, classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral */
