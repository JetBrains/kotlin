// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.declaredMemberProperties

class A(val foo: String)

fun box(): String {
    return (A::class.declaredMemberProperties.single()).invoke(A("OK")) as String
}
