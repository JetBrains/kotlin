package zzz

import JavaClass
import JavaClass.Z

class A : JavaClass() {
    fun test() = runZ(JavaClass.Z {a, b -> a + b})
}

fun box(): String {
    return A().test()
}