import posix.*

fun main(args: Array<String>) {
    // Just check the typealias is in scope.
    val ptrdiff: ptrdiff_t = 0L
    println("ptrdiff = $ptrdiff")
}
