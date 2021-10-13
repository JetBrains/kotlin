// WITH_REFLECT

import kotlin.reflect.KClass

// See KT-49076

@Deprecated("Use ...", ReplaceWith("bar", imports = arrayOf("my.package.bar")))
@Third(
    First(arrayOf(String::class)),
    Second(arrayOf(First(arrayOf(Int::class)), First(arrayOf(Double::class))))
)
// Incorrect array inside
@Second(<!ARGUMENT_TYPE_MISMATCH!>arrayOf(arrayOf(""), arrayOf(First(arrayOf())))<!>)
fun foo() {}

annotation class First(val value: Array<KClass<*>>)

annotation class Second(val value: Array<First>)

annotation class Third(val first: First, val second: Second)
