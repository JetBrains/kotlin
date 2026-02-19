// MODULE: context

// FILE: context.kt
fun test() {
    var x = 0
    <caret_context>val y = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
// FragmentSharedVariablesLowering depends on a specific function name
// CODE_FRAGMENT_METHOD_NAME: generated_for_debugger_fun
x = 1