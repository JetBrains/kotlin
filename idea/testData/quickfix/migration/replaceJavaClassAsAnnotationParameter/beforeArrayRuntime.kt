// "Replace Class<T> with KClass<T> in whole annotation" "true"
// WITH_RUNTIME

annotation class Ann(val arg: Array<Class<*>><caret>)

Ann(arg = array(javaClass<String>(), javaClass<Double>())) class MyClass
