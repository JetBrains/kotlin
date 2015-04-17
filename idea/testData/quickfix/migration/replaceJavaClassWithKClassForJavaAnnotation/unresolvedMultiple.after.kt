// "Replace javaClass<T>() with T::class" "true"
// ERROR: Unresolved reference: Err
// WITH_RUNTIME

Ann(javaClass<Err>(), Int::class) class MyClass1
