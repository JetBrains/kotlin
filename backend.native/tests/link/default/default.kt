import kotlinx.cinterop.signExtend
import platform.posix.*

fun main(args: Array<String>) {
    // Just check the typealias is in scope.
    val sizet: size_t = 0.signExtend<size_t>()
    println("sizet = $sizet")
}
