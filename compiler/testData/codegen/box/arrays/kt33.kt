
// KJS_WITH_FULL_RUNTIME
fun box () : String {
    val s = ArrayList<String>()
    s.add("foo")
    s[0] += "bar"
    return if(s[0] == "foobar") "OK" else "fail"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
