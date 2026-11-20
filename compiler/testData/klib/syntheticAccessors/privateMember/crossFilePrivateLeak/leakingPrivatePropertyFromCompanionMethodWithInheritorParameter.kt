// LANGUAGE: +CompanionBlocks
// FILE: A.kt
internal open class UndoManager<R>(private val o: String = "O", private val k: String = "K") {
    companion object {
        inline fun <reified T> getO(value: Derived<T>): String {
            val base: UndoManager<List<T>> = value
            return base.o
        }
    }

    companion {
        inline fun <reified T> getK(value: Derived<T>): String {
            val base: UndoManager<List<T>> = value
            return base.k
        }
    }
}

internal class Derived<S>(o: String = "O", k: String = "K") : UndoManager<List<S>>(o, k)

// FILE: main.kt
fun box() : String {
    return UndoManager.getO(Derived<Int>()) + UndoManager.getK(Derived<Int>())
}
