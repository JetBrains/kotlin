// WITH_STDLIB
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// MODULE: lib
package com.example.klib.serialization.diagnostics

fun movedToLib() {}

// MODULE: main(lib)
// FILE: foo.kt
package com.example.klib.serialization.diagnostics

private fun privateFunSeparateFiles() = Unit

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun foo(): Long = 0L<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun <T, K> T.topLevelParametrized(s: K): T = TODO()<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
internal fun topLevelInternalVararg(vararg i: Int) = 0<!>

typealias S = Map<String, Int>
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun S.typealiasReciever() = 0<!>

// FILE: main.kt
package com.example.klib.serialization.diagnostics

private fun privateFunSeparateFiles() = Unit

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun foo(): String = ""<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun foo(): Int = 0<!>

@Deprecated("This function moved to the 'lib' module", level = DeprecationLevel.HIDDEN)
fun movedToLib() {}

class A {
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN)
    fun <T> parameterized(a: T): T = a<!>

    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun <T> parameterized(a: T) {}<!>

    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN)
    private fun privateMethod(): Int = 0<!>

    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>private fun privateMethod(): Long = 0L<!>
}

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN)
private fun privateTopLevelFunction(): Int = 0<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>private fun privateTopLevelFunction(): Long = 0L<!>

fun main() {
    foo()
    movedToLib()

    // Test that the diagnostic is reported for declarations that are referenced before they are declared
    bar()
}

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun bar(): Long = 0L<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun bar(): Long = 1L<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun <T, K> T.topLevelParametrized(s: K): T = TODO()<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
internal fun topLevelInternalVararg(vararg i: Int) = 0<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun Map<String, Int>.typealiasReciever() = 0<!>