// KT-4878

import fieldAccessFromExtensionInTraitImpl as D

trait T {
    fun Int.foo(d: D) = d.result!!
}

class A : T {
    fun bar() = 42.foo(D())
}

fun box() = A().bar()
