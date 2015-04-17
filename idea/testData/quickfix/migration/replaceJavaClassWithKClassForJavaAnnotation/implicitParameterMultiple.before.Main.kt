// "Replace javaClass<T>() with T::class" "true"
// WITH_RUNTIME

Ann(javaClass(), javaClass(), *array(javaClass())<caret>, arg1 = javaClass(), arg2 = javaClass()) class MyClass
