// WITH_FIR_TEST_COMPILER_PLUGIN
// MODULE: context
// FILE: context.kt
package test

fun test() {
    <caret_context>Unit
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
@org.jetbrains.kotlin.plugin.sandbox.AllOpen
class Top {
    enum class NestedEnum {
        Entry;
    }

    class NestedClass
}
