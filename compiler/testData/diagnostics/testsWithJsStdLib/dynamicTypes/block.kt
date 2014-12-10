// !MARK_DYNAMIC_CALLS

fun test() {
    dynamic {
        <!DEBUG_INFO_DYNAMIC!>foo<!>()
        <!DEBUG_INFO_DYNAMIC!>bar<!>.<!DEBUG_INFO_DYNAMIC!>baz<!>(0)
    }
}

fun dynamic<T>(body: dynamic.() -> T): T {
    val topLevel = null
    return topLevel.body()
}