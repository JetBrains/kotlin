// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// MODULE: dep
// FILE: dependency.kt
class MyClass<caret>

// MODULE: main(dep)
// FILE: main.kt
class Context<caret_context>

