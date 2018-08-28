fun test() {
    val items = listOf<Any>()
    items.forEach { }
    items.forEach { item -> }
    items.forEach { doSomething(it) }
    items.forEach { item -> doSomething(item) }
    items.forEach { items.forEach { doSomething(it) } }
    items.forEach { items.forEach { thing -> doSomething(it); doSomething(thing) } }
}

fun doSomething(item: Any) {}