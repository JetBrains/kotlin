// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for object <anonymous>" "false"
// ACTION: Disable 'Split Property Declaration'
// ACTION: Edit intention settings
// ACTION: Remove unnecessary non-null assertion (!!)
// ACTION: Split property declaration

fun foo() {
    val a = object : Base(""<caret>!!) {

    }
}

open class Base(s: Any)