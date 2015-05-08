// "Replace Class<T> with KClass<T> in whole annotation" "true"
// WITH_RUNTIME

annotation class Ann(vararg val arg: Class<*><caret>)

Ann(javaClass<String>(), javaClass<Double>(), *array(javaClass<Char>())) class MyClass
