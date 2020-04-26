fun box(): String {
    class Local {
        var result = "Fail"
    }

    val l = Local()
    (Local::result).set(l, "OK")
    return (Local::result).get(l)
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
