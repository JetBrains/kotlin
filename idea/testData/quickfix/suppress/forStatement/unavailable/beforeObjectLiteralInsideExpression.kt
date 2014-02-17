// "class com.intellij.codeInspection.SuppressIntentionAction" "false"

fun foo() {
    val a = object : Base(""<caret>!!) {

    }
}

open class Base(s: Any)