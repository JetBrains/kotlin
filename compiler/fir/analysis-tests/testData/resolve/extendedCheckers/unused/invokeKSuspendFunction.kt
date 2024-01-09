// WITH_STDLIB
import kotlin.reflect.KSuspendFunction1

suspend fun foo(action: KSuspendFunction1<String, Int>): Int {
    val <!UNUSED_VARIABLE!>localAction<!> = action

    return localAction("hello")
}