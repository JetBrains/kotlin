// ISSUE: KT-60639

import kotlin.reflect.KClass

interface A

typealias OtherA = A
typealias OtherOtherA = OtherA

annotation class Ann(vararg val kClass: KClass<*>)

@Ann(A::class, OtherA::class, OtherOtherA::class)
class Test {
    fun get() = "OK"
}

fun box(): String {
    return Test().get()
}