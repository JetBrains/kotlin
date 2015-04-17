// "Replace javaClass<T>() with T::class in whole project" "true"
// WITH_RUNTIME

import java.lang.Number

Ann(arg = array(Int::class, String::class)) class MyClass1

Ann(arg = array<java.lang.Class<*>>(Number::class, String::class)) class MyClass2

Ann(arg = array<java.lang.Class<out kotlin.Comparable<*>>>(Int::class, String::class)) class MyClass3

Ann(arg = array<java.lang.Class<Int>>(Int::class)) class MyClass4
