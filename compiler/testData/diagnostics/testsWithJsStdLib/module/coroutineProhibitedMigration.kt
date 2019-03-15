import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun dummy(): String = <!DEPRECATION_ERROR!>suspendCoroutine<!> {
    it.resume("OK")
}