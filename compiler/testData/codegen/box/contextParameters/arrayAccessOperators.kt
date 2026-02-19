// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

data class MyContainer(var s: String)

context(_context0: Int)
operator fun MyContainer.get(index: Int): String? {
    return if (index == 0 && _context0 == 42) s else null
}

context(_context0: Int)
operator fun MyContainer.set(index: Int, value: String) {
    if (index != 0  || _context0 != 42) return
    s = value
}

fun box(): String {
    return with(42) {
        val myContainer = MyContainer("fail")
        myContainer[0] = "OK"
        myContainer[0] ?: "fail"
    }
}
