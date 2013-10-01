open class JA() {
    public var name: String = KA().getName()

    public open fun newKA(): KA? {
        return KA()
    }
}
