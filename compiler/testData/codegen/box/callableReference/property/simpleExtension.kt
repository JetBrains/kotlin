val String.id: String
    get() = this

fun box(): String {
    val pr = String::id

    if (pr.get("123") != "123") return "Fail value: ${pr.get("123")}"

    if (pr.name != "id") return "Fail name: ${pr.name}"

    return pr.get("OK")
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
