class Foo
fun baz(f: Foo.(i: Int, j: Int) -> Int) {}

fun main(args: Array<String>) {
    baz { i, j -> i + j }<caret>
}