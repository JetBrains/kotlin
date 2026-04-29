// DISABLE_IR_TYPE_PARAMETER_SCOPE_CHECKS: ANY

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
