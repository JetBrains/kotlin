// "class com.intellij.codeInspection.SuppressIntentionAction" "false"

fun foo() {
    object : Base(""<caret>!!) {

    }
}

open class Base(s: Any)