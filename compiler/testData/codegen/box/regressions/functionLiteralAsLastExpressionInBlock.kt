fun box(): String {
    val p: (String) -> Boolean = if (true) {
        { true }
    } else {
        { true }
    }
    return "OK"
}