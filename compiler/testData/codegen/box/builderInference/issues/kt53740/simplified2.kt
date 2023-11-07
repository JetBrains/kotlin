// ISSUE: KT-53740

class Inv<T: Any> {
    private lateinit var data: T
    fun get(): T = data
    fun set(data: T) { this.data = data }
}

class Out<out T: Any>(val inv: Inv<out T>) {
    fun get(): T = inv.get()
}

class In<in T: Any>(val inv: Inv<in T>) {
    fun set(data: T) = inv.set(data)
}

fun <T: Any> execute(
    first: In<T>.() -> Unit,
    second: Out<T>.() -> Unit
): Inv<T> {
    val inv = Inv<T>()
    In(inv).first()
    Out(inv).second()
    return inv
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
