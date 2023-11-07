// ISSUE: KT-53639

class Buildee<T> {
    fun yield(value: T) {}
}
fun <T> Buildee<T>.extension() {}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

fun <T> execute(
    lambda1: () -> Buildee<T>,
    lambda2: Buildee<T>.() -> Unit
): Buildee<T> {
    return lambda1().apply(lambda2)
}

class TargetType

@Suppress("BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION")
fun box(): String {
    // K2/JVM: java.lang.IllegalStateException: Cannot serialize error type: ERROR CLASS: Cannot infer argument for type parameter T
    // K2/Native & K2/WASM & K2/JS: org.jetbrains.kotlin.backend.common.linkage.issues.IrDisallowedErrorNode: Class found but error nodes are not allowed.
    val temp = execute(
        { build { yield(TargetType()) } },
        { extension() },
    )
    val result: Buildee<TargetType> = temp // K1: TYPE_MISMATCH (expected Buildee<TargetType>, actual Buildee<Any?>)
    return "OK"
}
