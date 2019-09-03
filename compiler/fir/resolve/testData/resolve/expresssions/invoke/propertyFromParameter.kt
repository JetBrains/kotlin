class Bar(name: () -> String) {
    val name = name()
}