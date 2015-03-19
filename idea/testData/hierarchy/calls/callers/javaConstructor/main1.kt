open class JA: KA {
    constructor() {

    }

    constructor(a: Int): super() {

    }

    public var name: String = KA().getName()

    public open fun newKA(): KA? {
        return KA()
    }
}

class JA2: KA() {

}