fun test() {
    val local: Container? = Container()
    <expr>local?.value</expr>
}

class Container {
    val value: Container? = null
}
