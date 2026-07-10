// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ContextParameters
// WITH_STDLIB

fun beginTest() {
    context(
        Sink<Throwable> {},
        Sink<Value> {}
    ) {
        object : Runnable {
            override fun run() {
                test()
            }
        }.run()
    }
}

context(
    one: Sink<Throwable>,
    two: Sink<Value>
)
fun test() {
    println(one)
    println(two)
}

fun interface Sink<T> {
    fun consume(value: T)
}

object Value

fun box(): String {
    beginTest()
    return "OK"
}
