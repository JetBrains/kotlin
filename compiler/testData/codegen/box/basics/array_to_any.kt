// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    foo().hashCode()

    return "OK"
}

fun foo(): Any {
    return Array<Any?>(0, { i -> null })
}
