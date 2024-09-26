// MODULE: main
// WITH_FIR_TEST_COMPILER_PLUGIN
// FILE: main.kt
package pack

@org.jetbrains.kotlin.plugin.sandbox.AllOpen
class F<caret>oo

// MODULE: restoreInContextOf(main)

fun restoreInContextOf() {
    <caret_restoreAt>
}
