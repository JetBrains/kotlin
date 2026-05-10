// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

import kotlin.reflect.jvm.isAccessible

@JvmInline
value class Z(val value: Int)

sealed class S(val z: Z)

fun box(): String {
    val ctor = S::class.constructors.first()
    ctor.isAccessible = true
    try {
        ctor.call(Z(1))
    } catch (e: InstantiationException) {
        return "OK"
    }
    return "Fail"
}
