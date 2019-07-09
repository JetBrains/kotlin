fun Number.test() {
    <caret>when {
        this is Int -> {}
        this is Long -> {}
        this is Short -> {}
    }
}