// TODO: This interface must be marked as "fun" ones that modifier is supported
interface MyRunnable {
    fun foo(x: Int): Boolean
}

fun foo(m: MyRunnable) {}

fun main() {
    foo(MyRunnable { x ->
        x > 1
    })

    foo(MyRunnable({ it > 1 }))

    val x = { x: Int -> x > 1 }

    foo(MyRunnable(x))
}
