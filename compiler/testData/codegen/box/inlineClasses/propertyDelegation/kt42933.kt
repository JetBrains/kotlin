// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES

class Delegate {
    operator fun getValue(t: Any?, p: Any): String = "OK"
}

inline class Kla1(val default: Int) {
    fun getValue(): String {
        val prop by Delegate()
        return prop
    }
}

fun box() = Kla1(1).getValue()