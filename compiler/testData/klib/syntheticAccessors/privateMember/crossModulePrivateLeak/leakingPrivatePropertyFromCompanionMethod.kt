// LANGUAGE: +CompanionBlocks
// MODULE: lib
// FILE: A.kt
internal class UndoManager<R>(private val o: String = "O", private val k: String = "K") {
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

// MODULE: main()(lib)
// FILE: main.kt
fun box() : String {
    return UndoManager.getO(UndoManager<Int>()) + UndoManager.getK(UndoManager<Int>())
}
