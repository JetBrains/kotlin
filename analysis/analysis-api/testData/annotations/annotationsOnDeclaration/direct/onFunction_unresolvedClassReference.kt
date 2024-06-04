// COMPILATION_ERRORS

import kotlin.reflect.KClass

annotation class A(val a: Int, val c: KClass<*>)

@A(1, Unknown::class)
fun fo<caret>o(): Int = 42