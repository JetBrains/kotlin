fun test() {
    bar(<caret>Int::foo)
}

fun Int.foo(x: Int, y: Int = 42) = x + y

fun bar(f: (Int, Int) -> Int) {}