fun Int.foo(x: Int, y: Int = 42) = x + y

fun bar(f: (Int, Int) -> Int) {}

fun test() {
    bar <caret>{ i, x -> i.foo(x) }
}
