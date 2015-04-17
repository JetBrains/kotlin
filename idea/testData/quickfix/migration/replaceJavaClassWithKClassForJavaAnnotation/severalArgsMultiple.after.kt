// "Replace javaClass<T>() with T::class" "true"
// WITH_RUNTIME

import java.lang

Ann(String::class, x = 2, arg = (Int::class), args = array((Any::class), lang.String::class)) class MyClass
