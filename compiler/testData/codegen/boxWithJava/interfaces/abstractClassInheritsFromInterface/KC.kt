// KT-3407 Implementing (in Java) an abstract Kotlin class that implements a trait does not respect trait method definition

interface  T {
    fun foo() = "OK"
}

abstract class KC: T {}

fun box() = ExtendsKCWithT.bar()
