// Test

import kotlin.reflect.KClass

interface A
interface B<T, R>

typealias OtherA = A
typealias OtherOtherA = OtherA
typealias OtherB<X, Y> = B<Y, X>

annotation class Ann(vararg val kClass: KClass<*>)

@Ann(A::class, OtherA::class, OtherOtherA::class, B::class, OtherB::class)
interface Test
