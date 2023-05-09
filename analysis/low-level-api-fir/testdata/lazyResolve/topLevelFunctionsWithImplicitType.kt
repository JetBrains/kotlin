fun resolve<caret>Me() {
    receive(functionWithLazyBody())
}

fun receive(value: String){}

fun functionWithLazyBody() = "42"