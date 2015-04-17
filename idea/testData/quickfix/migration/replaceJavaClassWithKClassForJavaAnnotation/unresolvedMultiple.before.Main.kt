// "Replace javaClass<T>() with T::class" "true"
// ERROR: Unresolved reference: Err
// WITH_RUNTIME

Ann(javaClass<Err>(), javaClass<Int>()<caret>) class MyClass1
