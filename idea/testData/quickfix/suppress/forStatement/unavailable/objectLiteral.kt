// "class com.intellij.codeInspection.SuppressIntentionAction" "false"
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for fun foo
// ACTION: Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for file objectLiteral.kt

fun foo() {
    object : Base(""<caret>!!) {

    }
}

open class Base(s: Any)