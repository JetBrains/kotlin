// ISSUE: KT-65103
interface Consumer<in T>

public fun <T> buildConsumer(
    block: (Consumer<T>) -> Unit
): T = "O" as T

public fun <T> materialize(): T = "K" as T

fun expectConsumerString(x: Consumer<String>) {}

fun foo1(x: Boolean) = when {
    x -> buildConsumer {
        expectConsumerString(it)
    }
    else -> materialize()
}

fun foo2(x: Boolean) =
    if (x)
        buildConsumer {
            expectConsumerString(it)
        }
    else
        materialize()

fun box(): String {
    return foo1(true) + foo2(false)
}
