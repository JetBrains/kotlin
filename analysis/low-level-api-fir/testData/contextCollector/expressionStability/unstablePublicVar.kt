fun test() {
    val local: Container? = Container()
    <expr>local?.value</expr>
}

class Container {
    var value: Container? = null
}
