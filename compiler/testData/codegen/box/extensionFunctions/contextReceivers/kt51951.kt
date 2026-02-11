// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_BACKEND_K2: ANY
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

typealias StringProvider = () -> String

context(StringProvider)
fun doSomething() {
    println(this@StringProvider())
}

fun box(): String {
    with({ "" }) {
        doSomething()
    }
    return "OK"
}