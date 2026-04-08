// WITH_STDLIB
// ISSUE: KT-76950

fun box(): String {
    val nullable: UByte? = 5.toUByte()
    val arr = if (nullable != null) ubyteArrayOf(nullable) else ubyteArrayOf()
    return if (arr.size == 1) "OK" else "Fail: $arr"
}
