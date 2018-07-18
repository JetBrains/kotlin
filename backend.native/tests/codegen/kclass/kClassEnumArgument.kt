package codegen.kclass.kClassEnumArgument

import kotlin.test.*
import kotlin.reflect.KClass

enum class E(val arg: KClass<*>?) {
    A(null as KClass<*>?),
    B(String::class);
}

@Test fun runTest() {
    println(E.B.arg?.simpleName)
}