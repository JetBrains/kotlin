// JVM_ABI_K1_K2_DIFF: KT-63984

// FILE: accessorForProtectedPropertyWithPrivateSetterWithIntermediateClass.kt
import a.A

open class A2 : A()

class B : A2() {
    fun test() = { -> vo + fk()() }
}

fun box() = B().test()()

// FILE: a.kt
package a

open class A {
    protected var vo = "O"
        private set

    protected var vk = ""
        private set

    fun fk() = { ->
        vk = "K"
        vk
    }
}
