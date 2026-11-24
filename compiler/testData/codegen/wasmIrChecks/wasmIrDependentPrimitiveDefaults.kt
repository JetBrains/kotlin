// TARGET_BACKEND: WASM
// ISSUE: KT-91749
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=setWords$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=fabsLike
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=zeroLow
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=setWords$default
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=fabsLike
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=zeroLow

fun highWord(x: Double): Int = (x.toRawBits() ushr 32).toInt()
fun lowWord(x: Double): Int = x.toRawBits().toInt()

fun setWords(d: Double = 0.0, hi: Int = highWord(d), lo: Int = lowWord(d)): Double =
    Double.fromBits((hi.toLong() shl 32) or (lo.toLong() and 0xffffffffL))

fun fabsLike(x: Double): Double = setWords(x, hi = highWord(x) and 0x7fffffff)
fun zeroLow(x: Double): Double = setWords(x, lo = 0)

fun box(): String {
    if (fabsLike(-1.5).toRawBits() != 1.5.toRawBits()) return "FAIL fabs"
    if (fabsLike(-0.0).toRawBits() != 0.0.toRawBits()) return "FAIL signed zero"
    val x = Double.fromBits((-1.5).toRawBits() or 1L)
    val explicit = setWords(0.0, highWord(x), 0)
    if (zeroLow(x).toRawBits() != explicit.toRawBits()) return "FAIL dependent hi"
    if (fabsLike(x).toRawBits() != setWords(0.0, highWord(x) and 0x7fffffff, lowWord(x)).toRawBits())
        return "FAIL dependent lo"
    return "OK"
}
