class Foo
fun bar(f: Foo.() -> Unit) {}

fun main(args: Array<String>) {
    bar {}<caret>
}