fun bar(f: () -> Unit) = f()

fun foo() {
    var v: Any
    bar { <!UNINITIALIZED_VARIABLE!>v<!>.hashCode() }
}
