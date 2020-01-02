fun use(x: Any?) = x

class Eap {
    private val foo = toString()

    constructor(foo: Int) {
        use(foo)
    }
    constructor(foo: String) {
        use(foo)
    }
}