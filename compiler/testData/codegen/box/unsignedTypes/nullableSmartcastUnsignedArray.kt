// WITH_STDLIB
// ISSUE: KT-76950
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:1.9,2.0,2.1,2.2
// ^^^ KT-76950 is fixed in 2.2.20-Beta1

fun box(): String {
    val nullable: UByte? = 5.toUByte()
    val arr = if (nullable != null) ubyteArrayOf(nullable) else ubyteArrayOf()
    return if (arr.size == 1) "OK" else "Fail: $arr"
}
