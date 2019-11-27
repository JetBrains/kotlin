// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME

object O {
    val mmmap = HashMap<String, Int>();

    init {
        fun doStuff() {
            mmmap.put("two", 2)
        }
        doStuff()
    }
}

fun box(): String {
    val r = O.mmmap["two"]
    if (r != 2) return "Fail: $r"
    return "OK"
}
