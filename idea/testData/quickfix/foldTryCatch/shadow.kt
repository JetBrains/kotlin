// "class org.jetbrains.kotlin.idea.quickfix.LiftAssignmentOutOfTryFix" "false"
// ACTION: Change to var
// ERROR: Val cannot be reassigned
// ERROR: Val cannot be reassigned
// WITH_RUNTIME
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

fun foo() {
    val x = 1
    try {
        val x = 2
        x = 3
    } catch(e: Exception) {
        <caret>x = 4
    }
}