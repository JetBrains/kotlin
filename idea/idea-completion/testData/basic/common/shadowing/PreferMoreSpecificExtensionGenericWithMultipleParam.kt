fun <T, S> List<T>.xxx(t: T){}
fun <T, W> Iterable<T>.xxx(t: T){}

fun foo() {
    listOf(1).xx<caret>
}

// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "(t: Int) for List<T> in <root>", typeText: "Unit" }
// NOTHING_ELSE
