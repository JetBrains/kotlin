fun test() {
    val items = listOf<Any>()
    items.forEach { }
    items.forEach { item -> }
    items.forEach { doSomething(it) }
    items.forEach { item -> doSomething(item) }
}

fun doSomething(item: Any) {}