// PROBLEM: none
fun <T> doSomething(a: T) {}

fun foo(x: Int) {
    if (x == 1) <caret>{
        doSomething(x)
    }
}
