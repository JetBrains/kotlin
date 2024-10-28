// MODULE: Context

// MODULE: Fragment
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: Context
// MAIN_MODULE

// FILE: fragment.kt

// CODE_FRAGMENT_KIND: FILE_LIKE

var x: String
    get() = 1
    s<caret>et(value) {
        field = v
    }