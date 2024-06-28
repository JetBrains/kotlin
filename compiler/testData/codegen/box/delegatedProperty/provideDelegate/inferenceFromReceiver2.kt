// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

object Inference2 {
    interface Foo<T>

    fun <T> delegate(): Foo<T> = object : Foo<T> {}

    operator fun <T> Foo<T>.provideDelegate(host: T, p: Any?): Foo<T> = this
    operator fun <T> Foo<T>.getValue(receiver: Inference2, p: Any?): String = "OK"

    val test1: String by delegate() // same story like in Inference1
    val test2: String by delegate<Inference2>()
}

fun box(): String {
    require(Inference2.test1 == "OK" && Inference2.test2 == "OK")
    return "OK"
}
