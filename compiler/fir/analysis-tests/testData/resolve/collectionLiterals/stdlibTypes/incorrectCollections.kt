// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTICS_FULL_TEXT

@OptIn(ExperimentalUnsignedTypes::class)
fun test() {
    val a: List<Int> <!INITIALIZER_TYPE_MISMATCH!>=<!> [1, null, 3]
    val b: MutableList<Any> <!INITIALIZER_TYPE_MISMATCH!>=<!> [null]
    val c: Set<String> <!INITIALIZER_TYPE_MISMATCH!>=<!> [1]
    val d: MutableSet<String> <!INITIALIZER_TYPE_MISMATCH!>=<!> ["" as CharSequence]
    val e: Sequence<Int> <!INITIALIZER_TYPE_MISMATCH!>=<!> [1L, 2L, 3L]

    val f: Array<Array<Int>> = [[<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>]]
    val g: IntArray = [<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>]
    val h: LongArray = [<!ARGUMENT_TYPE_MISMATCH!>0.0<!>]
    val i: ShortArray = [<!ARGUMENT_TYPE_MISMATCH!>1_000_000_000<!>]
    val j: ByteArray = [if (true) 42.toByte() else 42]
    val k: BooleanArray = [<!ARGUMENT_TYPE_MISMATCH!>Unit<!>]
    val l: FloatArray = [<!ARGUMENT_TYPE_MISMATCH!>0.0f as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Float<!><!>]
    val m: DoubleArray = [<!ARGUMENT_TYPE_MISMATCH!>42<!>, 42.0]

    val n: UIntArray = [<!ARGUMENT_TYPE_MISMATCH!>42<!>]
    val o: ULongArray = [<!ARGUMENT_TYPE_MISMATCH!>42L<!>]
    val p: UShortArray = [<!ARGUMENT_TYPE_MISMATCH!>42.toShort()<!>]
    val q: UByteArray = [<!ARGUMENT_TYPE_MISMATCH!>{}<!>]
}

/* GENERATED_FIR_TAGS: asExpression, classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral */
