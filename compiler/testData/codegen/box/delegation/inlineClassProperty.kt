// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.*

@JvmInline
value class Z(val value: Int)

interface I {
    var x: Z
}

object A : I {
    override var x: Z = Z(0)
}

object B : I by A

fun box(): String {
    val x = B::class.memberProperties.single { it.name == "x" } as KMutableProperty1<I, Z>
    if (x.get(B) != Z(0)) return "Fail x get"

    x.set(B, Z(1))
    if (x.get(B) != Z(1)) return "Fail x set"

    return "OK"
}
