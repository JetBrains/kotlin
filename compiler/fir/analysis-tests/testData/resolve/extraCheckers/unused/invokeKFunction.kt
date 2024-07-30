// WITH_STDLIB
import kotlin.reflect.KFunction1

fun foo(action: KFunction1<String, Int>): Int {
    val localAction = action

    return localAction("hello")
}