// ISSUE: KT-75844

package myPack

import myPack.MyObject.*

interface MyInterface<T> {
    val foo: T? get() = null
}

object MyObject : MyInterface<Int>

@Target(foo)
annotation class MyAn<caret>notation
