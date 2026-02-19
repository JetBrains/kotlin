package myPack

import myPack.MyObject.foo

interface MyInterface<T> {
    val foo: T? get() = null
}

object MyObject : MyInterface<Int>

@Target(foo)
annotation class MyAnn<caret>otation