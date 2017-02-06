@Suppress("NOTHING_TO_INLINE")
inline fun foo(body: () -> Unit) {
    println("hello1")
    body()
    println("hello4")
}

fun bar() {
    foo {
        println("hello2")
        println("hello3")
    }
}

fun main(args: Array<String>) {
    bar()
}
