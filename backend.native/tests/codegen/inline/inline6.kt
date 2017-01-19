//@Suppress("NOTHING_TO_INLINE")
//inline fun foo(body: () -> Unit) {
fun foo(body: () -> Unit) {
    body()
}

fun bar() {
    foo {
        println("hello")
    }
}

fun main(args: Array<String>) {
    bar()
}
