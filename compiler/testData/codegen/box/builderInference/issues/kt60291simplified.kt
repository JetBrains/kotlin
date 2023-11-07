// ISSUE: KT-60291

class Buildee<T> {
    fun yield(value: T) {}
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

fun <T> execute(vararg values: Buildee<T>) {}

class TargetType

@Suppress("BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION")
fun box(): String {
    // K2/JVM: java.lang.IllegalStateException: Cannot serialize error type: ERROR CLASS: Cannot infer argument for type parameter T
    // K2/Native & K2/WASM & K2/JS: org.jetbrains.kotlin.backend.common.linkage.issues.IrDisallowedErrorNode: Class found but error nodes are not allowed.
    execute(
        build { yield(TargetType()) },
        build { }
    )
    return "OK"
}
