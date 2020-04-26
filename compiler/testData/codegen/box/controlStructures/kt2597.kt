fun box(): String {
    var i = 0
    {
        if (1 == 1) {
            i++
        } else {
        }
    }()
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT
