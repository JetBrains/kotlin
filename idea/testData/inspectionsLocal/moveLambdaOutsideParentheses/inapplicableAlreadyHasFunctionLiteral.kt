// PROBLEM: none
fun foo() {
    bar(<caret>{ it }) {it}
}

fun bar(p1: (Int) -> Int, p2: (Int) -> Int) { }
