fun resolveMe() {
    receive(functionWithLazyBody())
}

fun receive(value: String){}

fun functionWithLazyBody(): String {
    return "42"
}