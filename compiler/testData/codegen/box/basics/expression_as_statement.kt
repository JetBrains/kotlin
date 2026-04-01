// WITH_STDLIB
import kotlin.test.*

fun foo() {
    Any() as String
}

fun box(): String {
    try {
        foo()
    } catch (e: Throwable) {
        return "OK"
    }

    return "Fail"
}
