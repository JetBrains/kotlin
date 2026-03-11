// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: FULL
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full
// MODULE: lib1
// MODULE_KIND: LibraryBinary
// FILE: MyLet.kt

import kotlin.contracts.*

@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

// MODULE: main(lib1)
// FILE: App.kt

// Check that contract 'survives' across modules:
fun main(s: String?, sb: StringBuilder) {
    s?.myLet { sb.append(it) }
    s?.myLet { sb.toString() + it }
}

// Check that adding a contract does not break existing callsInPlace: there should be no CAPTURED_VAL_INITIALIZATION.
fun foobar(s: String?, ss: String?) {
    val a: String
    s.myLet { a = ss!! }
    a.length
}
