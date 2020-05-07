// IS_APPLICABLE: false
fun test() {
    C().foo { <caret>i -> i + 1 }
}

class C {
    fun foo(f: (Int) -> Int) {}
    fun foo(f: (Int, Int) -> Int) {}
}