// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ERROR: Unresolved reference: TTT

val : Int
    get() {
        val t : TTT = null
        return 1
    }