// DO_NOT_CHECK_SYMBOL_RESTORE
// MODULE: dep
// FILE: dependency.kt
class Dep<caret_context>

// MODULE: main(dep)
// FILE: main.kt
val p<caret>rop = 0
