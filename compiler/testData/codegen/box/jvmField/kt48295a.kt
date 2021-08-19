// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: a.kt

package a

open class A {
    @JvmField protected var result = "Fail"
}

open class AA : A()

// FILE: b.kt

class B : a.AA() {
    fun test(): String {
        super.result = "OK"
        return super.result
    }
}

fun box(): String = B().test()
