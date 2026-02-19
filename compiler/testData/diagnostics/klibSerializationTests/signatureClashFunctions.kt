// WITH_STDLIB
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT
// RUN_PIPELINE_TILL: BACKEND

// MODULE: lib
package com.example.klib.serialization.diagnostics

fun movedToLib() {}

// MODULE: main(lib)
// FILE: foo.kt
package com.example.klib.serialization.diagnostics

private fun privateFunSeparateFiles() = Unit

@Deprecated("", level = DeprecationLevel.HIDDEN)
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun foo(): Long = 0L<!>

@Deprecated("", level = DeprecationLevel.HIDDEN)
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun <T, K> T.topLevelParametrized(s: K): T = TODO()<!>

@Deprecated("", level = DeprecationLevel.HIDDEN)
internal <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun topLevelInternalVararg(vararg i: Int) = 0<!>

typealias S = Map<String, Int>
@Deprecated("", level = DeprecationLevel.HIDDEN)
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun S.typealiasReciever() = 0<!>

// FILE: main.kt
package com.example.klib.serialization.diagnostics

private fun privateFunSeparateFiles() = Unit

@Deprecated("", level = DeprecationLevel.HIDDEN)
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun foo(): String = ""<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun foo(): Int = 0<!>

@Deprecated("This function moved to the 'lib' module", level = DeprecationLevel.HIDDEN)
fun movedToLib() {}

class A {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN)
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun <T> parameterized(a: T): T = a<!>

    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun <T> parameterized(a: T) {}<!>

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN)
    private <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun privateMethod(): Int = 0<!>

    private <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun privateMethod(): Long = 0L<!>
}

@Deprecated(message = "", level = DeprecationLevel.HIDDEN)
private <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun privateTopLevelFunction(): Int = 0<!>

private <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun privateTopLevelFunction(): Long = 0L<!>

fun main() {
    foo()
    movedToLib()

    // Test that the diagnostic is reported for declarations that are referenced before they are declared
    bar()
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun bar(): Long = 0L<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun bar(): Long = 1L<!>

@Deprecated("", level = DeprecationLevel.HIDDEN)
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun <T, K> T.topLevelParametrized(s: K): T = TODO()<!>

@Deprecated("", level = DeprecationLevel.HIDDEN)
internal <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun topLevelInternalVararg(vararg i: Int) = 0<!>

@Deprecated("", level = DeprecationLevel.HIDDEN)
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun Map<String, Int>.typealiasReciever() = 0<!>
