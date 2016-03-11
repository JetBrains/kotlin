fun <T> doSomething(a: T) {}

fun foo() {
    if (true) <caret>{
//        val a = 1
//        var b = 1
        doSomething("test")
    }
}
