// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: FULL
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full
// MODULE: lib1
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt

import kotlin.contracts.*

@OptIn(kotlin.contracts.ExperimentalContracts::class)
fun <T> id(x: T): T {
    contract {
        returnsParameter(x)
    }
    return x
}

@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <T> T.myApply(block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsParameter(this@myApply)
    }
    block()
    return this
}

// MODULE: main(lib1)
// FILE: App.kt

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

// Check that the contract 'survives' across modules:
fun main(s: String) {
    id(fooS())
    id(ign())

    fooS().myApply { }
    s.myApply { }
}
