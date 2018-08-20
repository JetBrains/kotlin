// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.full.declaredMemberProperties

class A(val foo: String)

fun box(): String {
    return (A::class.declaredMemberProperties.single()).invoke(A("OK")) as String
}
