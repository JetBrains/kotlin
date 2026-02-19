// ISSUE: KT-65103
interface Consumer<in T>

public fun <T> buildConsumer(
    b: Boolean,
    block: (Consumer<T>) -> Unit
): T? = if (b) ("O" as T) else null

public fun <T> materialize(): T = "K" as T

fun expectConsumerString(x: Consumer<String>) {}

fun elvis(b: Boolean) =
    buildConsumer(b) {
        expectConsumerString(it)
    } ?: materialize()

fun box(): String = elvis(true) + elvis(false)
