fun <T> doSomething(a: T) {}

fun foo(i: Int) {
    <caret>if (i == 1)
        doSomething(1)
    else
        doSomething(0)

    doSomething(-1)
}