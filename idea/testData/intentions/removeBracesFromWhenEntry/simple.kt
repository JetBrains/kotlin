fun foo() {
    when (1) {
        else -> {
            foo()<caret>
        }
    }
}