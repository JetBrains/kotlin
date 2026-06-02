class Container(var storage: String) {
    operator fun plusAssign(value: String) {
        storage += value
    }
}

fun test(container: Container) {
    <expr>container</expr> += "bar"
}
