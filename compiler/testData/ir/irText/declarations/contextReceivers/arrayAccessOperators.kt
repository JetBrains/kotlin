// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND: JS_IR

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57435

data class MyContainer(var s: String)

context(Int)
operator fun MyContainer.get(index: Int): String? {
    return if (index == 0 && this@Int == 42) s else null
}

context(Int)
operator fun MyContainer.set(index: Int, value: String) {
    if (index != 0  || this@Int != 42) return
    s = value
}

fun box(): String {
    return with(42) {
        val myContainer = MyContainer("fail")
        myContainer[0] = "OK"
        myContainer[0] ?: "fail"
    }
}
