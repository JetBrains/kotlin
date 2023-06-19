fun test() {
    var x = 0
    <caret>val y = 0
}

// FragmentSharedVariablesLowering depends on a specific function name
// CODE_FRAGMENT_METHOD_NAME: generated_for_debugger_fun