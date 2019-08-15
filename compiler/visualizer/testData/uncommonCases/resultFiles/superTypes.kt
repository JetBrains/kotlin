package org.jetbrains.kotlin.test

abstract class Base<T>(var x: T) {
    fun replace(newValue: T)
}

//                         constructor Base<T>(Int)
//                         │
class Derived(var x: Int): Base<Int>() {
    override fun replace(newValue: Int) {
//      var (Derived).x: Int
//      │   Derived.replace.newValue: Int
//      │   │
        x = newValue
    }
}

fun test() {
//  constructor Derived(Int)
//  │           fun (Derived).replace(Int): Unit
//  │       Int │       Int
//  │       │   │       │
    Derived(10).replace(20)
}
