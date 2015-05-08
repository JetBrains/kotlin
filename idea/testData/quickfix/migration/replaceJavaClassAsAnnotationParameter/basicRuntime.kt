// "Replace Class<T> with KClass<T> in whole annotation" "true"
// WITH_RUNTIME

annotation class Ann(val arg1: Class<*><caret>, val arg2: Class<out Any?>)

Ann(javaClass<String>(), javaClass<Int>()) class MyClass
