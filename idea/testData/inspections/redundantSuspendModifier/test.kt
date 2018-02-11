// WITH_RUNTIME

fun coroutine(block: suspend () -> Unit) {}

// Not redundant
suspend fun rootSuspend() {
    coroutine {
        empty()
    }
}

// Redundant
suspend fun empty() {}

suspend fun suspendUser() = rootSuspend() // not redundant

open class My {
    // Not redundant
    open suspend fun baseSuspend() {
        rootSuspend()
    }
}

class Your : My() {
    override fun baseSuspend() {

    }
}

class SIterable {
    operator fun iterator() = this
    // Redundant
    suspend operator fun hasNext(): Boolean = false
    // Redundant
    suspend operator fun next(): Int = 0
}

class SIterator {
    // Redundant
    suspend operator fun iterator() = this
    operator fun hasNext(): Boolean = false
    operator fun next(): Int = 0

}

// Not redundant
suspend fun foo() {
    val iterable = SIterable()
    coroutine {
        for (x in iterable) {
            println(x)
        }
    }
}

// Not redundant
suspend fun bar() {
    val iterator = SIterator()
    coroutine {
        for (x in iterator) {
            println(x)
        }
    }
}

interface Suspended {
    // Not redundant
    suspend fun bar()
}