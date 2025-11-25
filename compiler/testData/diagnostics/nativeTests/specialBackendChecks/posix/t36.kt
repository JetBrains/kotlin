// RUN_PIPELINE_TILL: BACKEND
// WITH_PLATFORM_LIBS
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun foo() = stat(malloc(42u)!!.rawValue)
