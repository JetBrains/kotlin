// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY

fun box(): String {
    with(0) {
        Child()
    }
    return "OK"
}

context(Int) open class Parent
context(Int) class Child : Parent()