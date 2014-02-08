fun foo(i: (Int) -> Unit) {}
fun test() {
    foo { <caret>x ->
        array(1, 2, 3).filter { x -> x % 2 == 0 }
    }
}
