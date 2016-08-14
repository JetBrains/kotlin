// IS_APPLICABLE: false

fun foo() {
    when (1) {
        else -> {
            val a = 1<caret>
        }
    }
}