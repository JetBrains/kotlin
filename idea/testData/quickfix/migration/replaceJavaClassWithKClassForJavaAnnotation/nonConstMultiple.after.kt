// "Replace javaClass<T>() with T::class" "true"
// ERROR: An annotation parameter must be a `javaClass<T>()` call
// WITH_RUNTIME

val jClass = javaClass<String>()
Ann(jClass, Int::class) class MyClass1
