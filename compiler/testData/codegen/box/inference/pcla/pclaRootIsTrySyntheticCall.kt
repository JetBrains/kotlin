// ISSUE: KT-65103
interface Consumer<in T>

public fun <T> buildConsumer(
    block: (Consumer<T>) -> Unit
): T = "OK" as T

fun expectConsumerString(x: Consumer<String>) {}

fun box() =
    try { // This try is essential
        buildConsumer {
            expectConsumerString(it)
        }
    } finally {
    }
