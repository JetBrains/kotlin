// "Replace Class<T> with KClass<T> in whole annotation" "true"
// WITH_RUNTIME

annotation class Ann(
    val arg1: Int,
    val arg2: Class<*><caret> = javaClass<Int>(),
    val arg3: Array<Class<out Any?>> = array(javaClass<String>()),
    vararg val arg4: Class<out Any?> = array(javaClass<Double>())
)

Ann(arg1 = 1) class MyClass1
Ann(arg1 = 2, arg2 = javaClass<Boolean>()) class MyClass2
Ann(arg1 = 3, arg3 = array(javaClass<Boolean>())) class MyClass3
Ann(arg1 = 4, arg4 = javaClass<String>()) class MyClass4
