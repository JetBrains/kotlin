// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66717

interface IVar {
    var z: Int
}

abstract class WithVarPrivateSet {
    final var z: Int = 42
        private set
}

class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING!>G2<!> : WithVarPrivateSet(), IVar {
    fun foo() {
        <!INVISIBLE_SETTER!>z<!> = 5
    }
}

fun main() {
    G2().foo()
}
