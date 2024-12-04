// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: p3/foo.kt
package p3

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

@MyInlineable
public fun Foo(
    text: @MyInlineable () -> Unit,
) {}

// MODULE: main(lib)
// FILE: main.kt
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable
import p3.Foo

@MyInlineable
public fun Bar() {
    Foo(
        text = {}, // @Composable invocations can only happen from the context of a @Composable function
    )
}
