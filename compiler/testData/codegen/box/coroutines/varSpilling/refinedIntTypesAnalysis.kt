// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// FULL_JDK

import java.nio.ByteBuffer

class Test {
    suspend fun discardSuspend(discarded0: Long, max: Long) {
        while (isClosedForRead) {
            // this assignment is required
            val rc = reading {
                true
            }

            if (!readSuspend(1)) break
        }
    }


    private inline fun reading(block: () -> Boolean): Boolean {
        setupStateForRead() ?: return false
        try {
            return block()
        } finally {
        }
    }

    val isClosedForRead = false

    private suspend fun readSuspend(size: Int): Boolean = true
    private fun setupStateForRead(): Any? = null
}

fun box() = "OK"