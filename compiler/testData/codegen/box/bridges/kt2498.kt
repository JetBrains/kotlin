// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: NATIVE

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
