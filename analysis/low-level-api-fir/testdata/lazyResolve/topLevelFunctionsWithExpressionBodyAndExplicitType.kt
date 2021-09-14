fun resolveMe() {
    receive(functionWithLazyBody())
}

fun receive(value: String){}

fun functionWithLazyBody(): String = "42"