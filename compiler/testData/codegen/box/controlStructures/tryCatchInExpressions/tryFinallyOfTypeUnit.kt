fun check(value: Any?) {
    if (value != Unit) throw AssertionError("Fail: $value")
}

fun box(): String {
    val x = 10
    check(try { } catch (e: Exception) { } finally { x })
    return "OK"
}
