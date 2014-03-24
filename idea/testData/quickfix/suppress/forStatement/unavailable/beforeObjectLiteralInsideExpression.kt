// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for fun foo
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for val a

fun foo() {
    val a = object : Base(""<caret>!!) {

    }
}

open class Base(s: Any)