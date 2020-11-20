class A {
    fun resolveMe() {
        receive(functionWithLazyBody())
    }

    val x: Int = 10
        get() = field
        set(value) {
            field = value
        }

    fun receive(value: String) {}

    fun functionWithLazyBody(): String {
        return "42"
    }
}