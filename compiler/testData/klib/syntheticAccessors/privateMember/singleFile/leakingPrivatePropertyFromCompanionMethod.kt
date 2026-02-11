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