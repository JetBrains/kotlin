// "Replace Class<T> with KClass<T> in whole annotation" "true"
// WITH_RUNTIME

import kotlin.reflect.KClass

annotation class Ann(val arg: Array<KClass<*>>)

Ann(arg = array(String::class, Double::class)) class MyClass
