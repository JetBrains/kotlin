fun foo(f: (Int) -> Int) = f(0)
fun bar(f: () -> Int) = f()

fun test() {
    foo { bar { (1 + 2) * <selection>it</selection> } }
}