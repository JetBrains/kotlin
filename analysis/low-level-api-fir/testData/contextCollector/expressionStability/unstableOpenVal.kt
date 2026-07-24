fun test(container: Container) {
    <expr>container.value</expr>
}

open class Container {
    open val value: String = ""
}
