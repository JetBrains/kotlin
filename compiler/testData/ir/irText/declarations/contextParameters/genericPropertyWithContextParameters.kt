// LANGUAGE: +ContextParameters
// ISSUE: KT-85896

class C<Z> {
    context(x: X, y: Y)
    var <X, Y> ctx: X
        get() = x
        set(value) {}
}
