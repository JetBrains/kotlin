// JVM_ABI_K1_K2_DIFF: KT-63920

sealed class Sealed(val value: String) {
    constructor() : this("OK")
}

class Derived : Sealed()

fun box() = Derived().value