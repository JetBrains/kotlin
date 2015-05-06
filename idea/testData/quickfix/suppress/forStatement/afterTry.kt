// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo() {
    try {
        @suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        ""<caret>!!
    }
    finally {

    }
}