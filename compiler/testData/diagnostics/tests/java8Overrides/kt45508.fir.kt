interface A {
    fun foo(): Any
}

interface B {
    fun foo(): String = "A"
}

open class D: B

open class C: D(), A

// ------------

class Test: Impl(), CProvider

open class CC

class DD: CC()

interface CProvider {
    fun getC(): CC
}

interface DProvider {
    fun getC(): DD = DD()
}

open class Impl: DProvider
