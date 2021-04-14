fun interface MyRunnable {
    fun foo(x: Int): Boolean
}

fun foo(m: MyRunnable) {}

private fun interface PrivateRunnable {
    fun bar(x: String): Boolean
}

private fun bar(pr: PrivateRunnable) {}

fun main() {
    foo(MyRunnable { x ->
        x > 1
    })

    foo(MyRunnable({ it > 1 }))

    val x = { x: Int -> x > 1 }

    foo(MyRunnable(x))

    bar(PrivateRunnable { s -> s.length > 0 })
}
