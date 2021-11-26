// WITH_STDLIB

fun box(): String {
    val m = hashMapOf<String, String?>()
    m.put("b", null)
    val oldValue = m.getOrPut("b", { "Foo" })
    return if (oldValue == "Foo") "OK" else "fail: $oldValue"
}
