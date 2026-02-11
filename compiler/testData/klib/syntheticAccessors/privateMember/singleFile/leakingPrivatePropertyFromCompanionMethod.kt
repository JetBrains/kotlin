// IGNORE_BACKEND: JS_IR, WASM_JS, NATIVE
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: JS_IR, WASM_JS, NATIVE
internal class UndoManager<R>(private val capacity: String = "OK") {
    companion object {
        inline fun <reified T> getValue(value: UndoManager<T>): String {
            return value.capacity
        }
    }
}

fun box() : String {
    return UndoManager.getValue(UndoManager<Int>())
}