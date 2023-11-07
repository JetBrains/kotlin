// ISSUE: KT-53740

class Buildee<T: Any> {
    private lateinit var data: T
    fun get(): T = data
    fun set(data: T) { this.data = data }
}

fun <T: Any> execute(
    lambda1: Buildee<T>.() -> Unit,
    lambda2: Buildee<T>.() -> Unit
): Buildee<T> {
    val buildee = Buildee<T>()
    buildee.lambda1()
    buildee.lambda2()
    return buildee
}

@Suppress("BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION")
fun box(): String {
    execute(
        {
            set("")
        },
        {
            // K1/JVM & K1/WASM & K2/JVM & K2/WASM: run-time failure (java.lang.ClassCastException)
            val value: Int = get()
        }
    )
    return "OK"
}
