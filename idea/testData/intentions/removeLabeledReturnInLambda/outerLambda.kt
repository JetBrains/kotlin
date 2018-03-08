// WITH_RUNTIME
// IS_APPLICABLE: FALSE

fun foo() {
    listOf(1,2,3).forEach {
        listOf(1,2,3).find {
            return@forEach<caret>
        }
    }
}