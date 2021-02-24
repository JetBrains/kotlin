// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_STRING_BUILDER
// KJS_WITH_FULL_RUNTIME
class A {
    private val sb: StringBuilder = StringBuilder()

    operator fun String.unaryPlus() {
        sb.append(this)
    }

    fun foo(): String {
        +"OK"
        return sb.toString()!!
    }
}

fun box(): String = A().foo()
