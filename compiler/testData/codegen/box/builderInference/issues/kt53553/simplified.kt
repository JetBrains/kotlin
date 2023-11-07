// ISSUE: KT-53553

class Buildee<T>

fun <T> execute(
    lambda1: Buildee<T>.(T) -> Unit,
    lambda2: Buildee<T>.(T) -> Unit
): T {
    val buildee = Buildee<T>()
    val value = Base() as T
    buildee.lambda1(value)
    buildee.lambda2(value) // K1/JVM & K1/WASM & K2/JVM & K2/WASM: run-time failure (java.lang.ClassCastException)
    return value
}

open class Base
class Derived: Base()

fun consumeBase(value: Base) {}
fun consumeDerived(value: Derived) {}

@Suppress("BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION")
fun box(): String {
    execute(
        {
            consumeBase(it)
        },
        {
            consumeDerived(it)
        }
    )
    return "OK"
}
