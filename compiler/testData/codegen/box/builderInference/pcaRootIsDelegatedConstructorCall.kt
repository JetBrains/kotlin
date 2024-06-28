// ISSUE: KT-65103
interface Consumer<in T>

public fun <T> buildConsumer(
    block: (Consumer<T>) -> Unit
): Any?  = "OK"

fun expectConsumerString(x: Consumer<String>) {}

abstract class A(val x: Any?)

class B : A(buildConsumer {
    expectConsumerString(it)
})

fun box(): String = B().x as String
