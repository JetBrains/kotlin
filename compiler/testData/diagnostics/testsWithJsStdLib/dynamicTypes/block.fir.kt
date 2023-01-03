// !MARK_DYNAMIC_CALLS

fun test() {
    dynamic {
        foo()
        bar.baz(0)
    }
}

fun <T> dynamic(body: dynamic.() -> T): T {
    val topLevel = null
    return topLevel.body()
}
