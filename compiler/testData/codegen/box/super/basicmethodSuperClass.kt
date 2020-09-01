// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: NATIVE

class N() : ArrayList<Any>() {
    override fun add(el: Any)  : Boolean {
        if (!super<ArrayList>.add(el)) {
            throw Exception()
        }
        return false
    }
}

fun box(): String {
    val n = N()
    if (n.add("239")) return "fail"
    if (n.get(0) == "239") return "OK";
    return "fail";
}
