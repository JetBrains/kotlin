open class JA() {
    public var name: String = KA().foo("")

    public open fun foo(): String {
        return KA().foo("")
    }
}
