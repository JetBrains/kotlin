open class JA() {
    public var name: String = "A"

    public open fun foo(s: String): String {
        System.out.println(s)
        return "A " + s
    }
}