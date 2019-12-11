fun bar(f: () -> Unit) = f()

fun foo() {
    var v: Any
    bar { v.hashCode() }
}
