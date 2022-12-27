// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

fun box(): String {
    with(0) {
        Child()
    }
    return "OK"
}

context(Int) open class Parent
context(Int) class Child : Parent()