import kotlinx.cinterop.convert
import platform.posix.*

fun foo() {
    println("linked library")
    val size: size_t = 17.convert<size_t>()
    val e = fabs(1.toDouble())
    println("and symbols from posix available: $size; $e")
}
