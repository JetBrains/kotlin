fun some(x: Any) {
    when (x) {
        is Int ->
            0
        else -> {
            1
        }
    }
}