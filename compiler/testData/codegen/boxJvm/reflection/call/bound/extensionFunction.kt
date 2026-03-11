// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*

fun String.foo(x: String) = this + x
fun String?.bar(x: String) = x

fun box() =
        (""::foo).call("O") + (null::bar).call("K")
