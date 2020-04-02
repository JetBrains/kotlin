// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.full.*

open class A(private val p: Int)
class B : A(42)

fun box() =
        if (B::class.memberProperties.isEmpty()) "OK"
        else "Fail: invisible fake overrides should not appear in KClass.memberProperties"
