// WITH_STDLIB
// ISSUE: KT-89919
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.4

fun box(): String {
    val fn = ::uintArrayOf
    val result = fn(uintArrayOf(42u))
    if (result[0] != 42u) return "Fail: 42 is expected, got ${result[0]}"

    val fnULong: (ULongArray) -> ULongArray = ::ulongArrayOf
    val resultULong = fnULong(ulongArrayOf(1uL shl 40, ULong.MAX_VALUE))
    if (!resultULong.contentEquals(ulongArrayOf(1uL shl 40, ULong.MAX_VALUE))) return "Fail ulong"

    return "OK"
}
