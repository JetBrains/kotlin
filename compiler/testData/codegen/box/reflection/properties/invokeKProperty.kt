// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.declaredMemberProperties

class A(val foo: String)

fun box(): String {
    return (A::class.declaredMemberProperties.single()).invoke(A("OK")) as String
}
