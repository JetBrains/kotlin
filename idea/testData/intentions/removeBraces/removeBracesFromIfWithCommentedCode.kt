fun doSomething<T>(a: T) {}

fun foo() {
    <caret>if (true) {
        //        val a = 1
        //        var b = 1
        doSomething("test")
    }
}
