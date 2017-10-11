import platform.posix.*

fun foo() {
    println("linked library")
    val size: size_t = 17L
    val e = fabs(1.toDouble())
    println("and symbols from posix available: $size; $e")
}
