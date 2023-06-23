// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTION_INHERITANCE
// KJS_WITH_FULL_RUNTIME
// DONT_TARGET_EXACT_BACKEND: NATIVE
class MySet : HashSet<Int>() {
    override fun remove(element: Int): Boolean {
        return super.remove(element)
    }
}

fun box(): String {
    val a = MySet()
    a.add(1)
    a.add(2)
    a.add(3)

    if (!a.remove(1)) return "fail 1"
    if (a.remove(1)) return "fail 2"
    if (a.contains(1)) return "fail 3"

    return "OK"
}
