// !OPT_IN: kotlin.js.ExperimentalJsExport kotlin.ExperimentalUnsignedTypes
// FIR_IDENTICAL

@JsExport
fun any(): Any? = null
@JsExport
fun <T> generic(): T? = null
@JsExport
fun nothing(): Nothing? = null
@JsExport
fun throwable(): Throwable? = null

<!NON_EXPORTABLE_TYPE!>@JsExport
fun unit(): Unit?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun char(): Char?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun charSeq(): CharSequence?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun number(): Number?<!> = null

@JsExport
fun str(): String? = null
@JsExport
fun bool(): Boolean? = null
@JsExport
fun byte(): Byte? = null
@JsExport
fun short(): Short? = null
@JsExport
fun int(): Int? = null
@JsExport
fun double(): Double? = null
@JsExport
fun float(): Float? = null

<!NON_EXPORTABLE_TYPE!>@JsExport
fun ubyte(): UByte?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun ushort(): UShort?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun uint(): UInt?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun long(): Long?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun ulong(): ULong?<!> = null

@JsExport
fun arr(): Array<Int>? = null
@JsExport
fun boolArr(): BooleanArray? = null
@JsExport
fun byteArr(): ByteArray? = null
@JsExport
fun shortArr(): ShortArray? = null
@JsExport
fun intArr(): IntArray? = null
@JsExport
fun longArr(): LongArray? = null
@JsExport
fun floatArr(): FloatArray? = null
@JsExport
fun doubleArr(): DoubleArray? = null

<!NON_EXPORTABLE_TYPE!>@JsExport
fun ubyteArr(): UByteArray?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun ushortArr(): UShortArray?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun uintArr(): UIntArray?<!> = null
<!NON_EXPORTABLE_TYPE!>@JsExport
fun ulongArr(): ULongArray?<!> = null
