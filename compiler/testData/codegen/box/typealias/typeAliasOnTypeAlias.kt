// ISSUE: KT-60639
// JVM_ABI_K1_K2_DIFF: KT-63921

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