public fun <T, R> myWith(receiver: T, block: T.() -> R): R {
    return receiver.block()
}

object Foo2 {
    operator fun Any?.get(key: String) = "OK"
}

object Main {
    fun bar() = myWith(Foo2) {

        val x = object {
            val y = 38["Hello!"]
        }
        x.y
    }
}

fun box(): String {
    return Main.bar()
}