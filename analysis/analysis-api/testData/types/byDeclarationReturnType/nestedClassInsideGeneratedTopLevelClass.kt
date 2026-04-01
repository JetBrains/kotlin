// WITH_FIR_TEST_COMPILER_PLUGIN
// FILE: source.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.ExternalClassWithNested

@ExternalClassWithNested
class MyClass

// FILE: main.kt
package foo

fun tes<caret>t(): AllOpenGenerated.NestedMyClass = TODO()
