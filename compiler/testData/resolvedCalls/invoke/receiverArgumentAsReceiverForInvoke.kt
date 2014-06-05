class Foo
fun Foo.invoke() {}

fun bar(f: Foo) {
    f<caret>()
}
