// MODULE: main
// WITH_FIR_TEST_COMPILER_PLUGIN
// FILE: main.kt
package foo

@org.jetbrains.kotlin.plugin.sandbox.MyInterfaceSupertype
class F<caret>oo

interface MyInterface

// MODULE: restoreInContextOf(main)

fun restoreInContextOf() {
    <caret_restoreAt>
}
