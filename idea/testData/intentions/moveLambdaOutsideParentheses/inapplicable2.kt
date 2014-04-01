// IS_APPLICABLE: false

fun doSomething<T>(a: T) {}

fun foo(x: Int) {
    if (x == 1) <caret>{
        doSomething(x)
    }
}
