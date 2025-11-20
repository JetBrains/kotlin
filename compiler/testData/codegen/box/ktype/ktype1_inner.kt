// WITH_STDLIB
// WITH_REFLECT


import kotlin.test.*
import kotlin.reflect.*

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified R> kType() = typeOf<R>()

class D
class Outer<T> {
    companion object Friend
    inner class Inner<S>
}

fun box(): String {
    val innerKType = kType<Outer<D>.Inner<String>>()
    assertEquals(Outer.Inner::class, innerKType.classifier)
    assertEquals(String::class, innerKType.arguments.first().type!!.classifier)
    assertEquals(D::class, innerKType.arguments.last().type!!.classifier)

    return "OK"
}
