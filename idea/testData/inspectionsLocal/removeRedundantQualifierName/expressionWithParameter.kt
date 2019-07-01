package my.simple.name

fun run() {}
fun go(check: () -> Unit) = check()

fun main() {
    val a = my.simple.name<caret>.go {
        run()
    }
    val b = my.simple.name.go(::run)
}