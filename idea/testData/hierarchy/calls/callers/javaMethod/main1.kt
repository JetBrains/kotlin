open class JA() {
    public var name: String = KBase().foo("") + KA().foo("")

    public open fun foo(): String {
        return KBase().foo("") + KA().foo("")
    }
}