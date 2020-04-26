data class Box(val value: String)

val foo = Box("lol")

fun box(): String {
    val property = ::foo
    if (property.get() != Box("lol")) return "Fail value: ${property.get()}"
    if (property.name != "foo") return "Fail name: ${property.name}"
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
