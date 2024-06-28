// WITH_STDLIB
// ISSUE: KT-65262

interface Consumer<in T>

public fun <T> buildConsumer(
    block: (Consumer<T>) -> Unit
): T = "OK" as T

fun expectConsumerString(x: Consumer<String>) {}

fun box() =
    try {
        buildConsumer { x ->
            val y by lazy {
                expectConsumerString(x)
                "OK"
            }

            if (y.length != 2) throw RuntimeException("fail 1")
        }
    } finally {
    }
