// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ERROR: Unresolved reference: TTT

val : Int
    get() {
        val t : <caret>TTT = null
        return 1
    }