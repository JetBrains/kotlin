// MODULE: main
// WITH_FIR_TEST_COMPILER_PLUGIN
// FILE: main.kt
package foo

@org.jetbrains.kotlin.fir.plugin.MyInterfaceSupertype
class F<caret>oo

interface MyInterface

// MODULE: restoreInContextOf(main)

fun restoreInContextOf() {
    <caret_restoreAt>
}