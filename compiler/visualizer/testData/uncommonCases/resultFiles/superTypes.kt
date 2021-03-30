package org.jetbrains.kotlin.test

abstract class Base<T>(var x: T) {
    abstract fun replace(newValue: T)
}

//                     constructor Base<T>(T)
//                     │         Derived.<init>.x: Int
//                     │         │
class Derived(x: Int): Base<Int>(x) {
    override fun replace(newValue: Int) {
//      var (Base<T>).x: T
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
