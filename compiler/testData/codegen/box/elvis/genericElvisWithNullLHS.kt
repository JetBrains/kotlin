// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun test() {
    fun returnMutableList(): MutableList<Int>? = null
    fun returnsList(): List<Int>? = null

    var mutableList: MutableList<Int>? = null
    var list: List<Int>? = null

    mutableListOf<Int>().addAll(returnMutableList() ?: emptyList<Int>())
    mutableListOf<Int>().addAll(returnsList() ?: emptyList())
    mutableListOf<Int>().addAll(list ?: emptyList())

    mutableListOf<Int>().addAll(returnMutableList() ?: emptyList())
    mutableListOf<Int>().addAll(mutableList ?: emptyList())
    mutableListOf<Int>().addAll(null ?: emptyList())
}

fun box(): String {
    test()
    return "OK"
}