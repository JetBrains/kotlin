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



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_STRING_BUILDER
