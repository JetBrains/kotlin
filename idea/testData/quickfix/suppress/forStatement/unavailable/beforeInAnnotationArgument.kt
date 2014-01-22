// "class com.intellij.codeInspection.SuppressIntentionAction" "false"

[Ann(Integer.MAX_VALUE<caret> + 1)]
fun foo() {}

annotation class Ann(val b: Int)