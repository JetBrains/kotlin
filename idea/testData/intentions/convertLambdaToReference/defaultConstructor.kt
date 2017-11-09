class Foo
fun usage(f: () -> Foo) {}
fun test() {
    usage {<caret> Foo() }
}