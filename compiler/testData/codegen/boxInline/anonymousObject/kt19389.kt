// FILE: 1.kt
public inline fun <T, R> myWith(receiver: T, block: T.() -> R): R {
    return receiver.block()
}

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING
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