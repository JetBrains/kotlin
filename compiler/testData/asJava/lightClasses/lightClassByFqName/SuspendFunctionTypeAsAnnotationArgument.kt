// test.MyClass
// WITH_STDLIB
package test

import kotlin.reflect.KClass

annotation class Ann(val kClass: KClass<*>)

class MyClass {
    @Ann(kotlin.coroutines.SuspendFunction0::class)
    fun suspend0() {}

    @Ann(kotlin.coroutines.SuspendFunction1::class)
    fun suspend1() {}

    @Ann(kotlin.coroutines.SuspendFunction21::class)
    fun suspend21() {}

    @Ann(kotlin.coroutines.SuspendFunction22::class)
    fun suspend22() {}
}
