fun foo(f: (Int) -> Int) = f(0)

fun test() {
    foo { foo { (1 + 2) * <selection>it</selection> } }
}