// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo() {
    @suppress("Foo") ""<caret>!!
}

annotation class ann