// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for object <anonymous>" "false"
// ACTION: Remove unnecessary non-null assertion (!!)

fun foo() {
    object : Base(""<caret>!!) {

    }
}

open class Base(s: Any)