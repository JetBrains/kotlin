fun foo(func: Int.(Int) -> Int) {}

fun test() {
    foo {
        this + it
    }
}