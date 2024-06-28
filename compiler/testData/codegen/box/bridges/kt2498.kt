// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTION_INHERITANCE
// KJS_WITH_FULL_RUNTIME
// DONT_TARGET_EXACT_BACKEND: NATIVE

open class BaseStringList: ArrayList<String>() {
}

class StringList: BaseStringList() {
    public override fun get(index: Int): String {
        return "StringList.get()"
    }
}

fun box(): String {
    val myStringList = StringList()
    myStringList.add("first element")
    if (myStringList.get(0) != "StringList.get()") return "Fail #1"
    val b: BaseStringList = myStringList
    val a: ArrayList<String> = myStringList
    if (b.get(0) != "StringList.get()") return "Fail #2"
    if (a.get(0) != "StringList.get()") return "Fail #3"
    return "OK"
}
