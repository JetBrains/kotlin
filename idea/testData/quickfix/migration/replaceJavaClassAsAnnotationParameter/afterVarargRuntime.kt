// "Replace Class<T> with KClass<T> in whole annotation" "true"
// WITH_RUNTIME

import kotlin.reflect.KClass

annotation class Ann(vararg val arg: KClass<*>)

Ann(String::class, Double::class, *array(Char::class)) class MyClass
