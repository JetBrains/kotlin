// WITH_FIR_TEST_COMPILER_PLUGIN

// MODULE: context
// FILE: context.kt

fun test() {
    <caret_context>val x = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
@org.jetbrains.kotlin.fir.plugin.AllOpen
class Foo {
    fun call() {}
}

class Bar : Foo() {}

Bar().call()