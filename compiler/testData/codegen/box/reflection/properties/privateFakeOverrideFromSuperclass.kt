// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.*

open class A(private val p: Int)
class B : A(42)

fun box() =
        if (B::class.memberProperties.isEmpty()) "OK"
        else "Fail: invisible fake overrides should not appear in KClass.memberProperties"
