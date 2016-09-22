// IS_APPLICABLE: false

fun bar() {}
fun gav() {}

fun foo() {
    when (1) {
        else -> {
            bar()<caret>
            gav()
        }
    }
}