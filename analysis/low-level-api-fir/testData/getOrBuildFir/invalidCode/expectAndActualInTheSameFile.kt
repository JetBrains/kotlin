// LANGUAGE: +MultiPlatformProjects
fun main() {
    println("Hello, Kotlin/Native!")
}

expect fun f()

fun test() {
}

actual fun f() { <expr>println("Hello")</expr> }


