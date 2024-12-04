// ISSUE: KT-66256

fun interface Element {
    fun invoke()
}

class Container<T> {
    fun add(arg: T) {}
}

fun test(c: Container<in Element>) {
    c.add({})
}

fun box(): String {
    test(Container())
    return "OK"
}
