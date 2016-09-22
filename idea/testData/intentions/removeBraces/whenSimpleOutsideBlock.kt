fun foo() {
    when (1) {
        else<caret> -> {
            foo()
        }
    }
}