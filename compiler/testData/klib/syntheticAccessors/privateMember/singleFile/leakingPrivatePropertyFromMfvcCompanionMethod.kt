// LANGUAGE: +CompanionBlocks +FullValueClasses
internal value class UndoManager<R>(private val o: String = "O", private val k: String = "K") {
    companion object {
        inline fun <reified T> getO(value: UndoManager<T>): String {
            return value.o
        }
    }

    companion {
        inline fun <reified T> getK(value: UndoManager<T>): String {
            return value.k
        }
    }
}

fun box() : String {
    return UndoManager.getO(UndoManager<Int>()) + UndoManager.getK(UndoManager<Int>())
}
