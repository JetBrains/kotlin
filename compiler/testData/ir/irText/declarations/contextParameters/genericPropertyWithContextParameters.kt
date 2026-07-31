// LANGUAGE: +ContextParameters
// ISSUE: KT-85896

class C<Z> {
    context(x: X, y: Y)
    var <X, Y> ctx: X
        get() = x
        set(value) {}
}

class A<Z> {
    class N<T> {
        context(x: X, y: Y)
        var <X, Y> ctx: X
            get() = x
            set(value) {}
    }
}

class B<Z> {
    context(x: X, y: Map<X, Y>)
    var <X, Y> ctx: X
        get() = x
        set(value) {}
}
