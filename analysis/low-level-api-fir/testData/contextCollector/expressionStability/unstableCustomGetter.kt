fun test(container: Container) {
    <expr>container.value</expr>
}

class Container {
    val value: String
        get() = ""
}
