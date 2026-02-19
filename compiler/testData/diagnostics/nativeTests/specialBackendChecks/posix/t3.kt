// RUN_PIPELINE_TILL: BACKEND
// WITH_PLATFORM_LIBS
import platform.posix.*

fun main() {
    println(::printf)
}
