fun foo(f: (Mutable) -> Unit) {}
fun foo(f: T.(Mutable) -> Unit) {}
fun foo(f: Array<(out) -> Unit>) {}
fun foo(f: Array<T.(out) -> Unit>) {}