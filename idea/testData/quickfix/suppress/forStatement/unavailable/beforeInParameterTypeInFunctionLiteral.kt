// "class com.intellij.codeInspection.SuppressIntentionAction" "false"

fun foo() {
    any {
        (x: String?<caret>?) ->
    }
}

fun any(a: Any?) {}