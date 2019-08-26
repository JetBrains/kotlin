// IGNORE_BACKEND: WASM
operator fun Int?.inc() : Int { if (this != null) return this.inc() else throw NullPointerException() }

public fun box() : String {
    var i : Int? = 10
    val j = i++

    return if(j==10 && 11 == i) "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ NullPointerException 
