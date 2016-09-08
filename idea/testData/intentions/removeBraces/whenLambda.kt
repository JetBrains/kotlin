// IS_APPLICABLE: false

fun foo() {
    when (1) {
        else -> {
            <caret>it: Int -> it.hashCode()
        }
    }
}