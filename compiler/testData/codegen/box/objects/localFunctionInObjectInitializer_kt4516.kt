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



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_HASH_MAP
