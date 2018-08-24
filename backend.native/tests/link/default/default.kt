import kotlinx.cinterop.convert
import platform.posix.*

fun main(args: Array<String>) {
    // Just check the typealias is in scope.
    val sizet: size_t = 0.convert<size_t>()
    println("sizet = $sizet")
}
