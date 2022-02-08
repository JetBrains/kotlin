fun foo() {}
fun bar(s: String) {}
fun bar(f: () -> Unit) {}
fun test() {
    foo(1)
    bar(2)
    bar("", 1)
    bar()
}
