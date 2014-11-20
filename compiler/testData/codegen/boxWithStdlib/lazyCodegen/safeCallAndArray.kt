class C {
    fun calc() : String {
        return "OK"
    }
}

fun box(): String? {
    val c: C? = C()
    val arrayList = array(c?.calc(), "")
    return arrayList[0]
}
