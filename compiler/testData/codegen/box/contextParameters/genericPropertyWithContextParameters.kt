// WITH_STDLIB
// LANGUAGE: +ContextParameters

// ISSUE: KT-85896

class C<Z> {
    context(x: X, y: Y)
    var <X, Y> ctx: Map<X, Y>
        get() = mapOf<X, Y>()
        set(value) {}
}

fun box(): String {
    val c = C<String>()
    return "OK"
}
