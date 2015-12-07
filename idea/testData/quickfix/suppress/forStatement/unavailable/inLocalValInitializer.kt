// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for fun foo
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for val bar
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for file inLocalValInitializer.kt

fun foo() {
    val bar = ""<caret>!!
}