// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

import kotlin.reflect.full.declaredMemberProperties

class A(val foo: String)

fun box(): String {
    return (A::class.declaredMemberProperties.single()).invoke(A("OK")) as String
}
