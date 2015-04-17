// "Replace javaClass<T>() with T::class in whole project" "true"
// WITH_RUNTIME

Ann(arg = array(javaClass<Int>(), javaClass<String>()<caret>)) class MyClass1

Ann(arg = array<java.lang.Class<*>>(javaClass<java.lang.Number>(), javaClass<String>())) class MyClass2

Ann(arg = array<java.lang.Class<out kotlin.Comparable<*>>>(javaClass<kotlin.Int>(), javaClass<String>())) class MyClass3

Ann(arg = array<java.lang.Class<Int>>(javaClass<kotlin.Int>())) class MyClass4
