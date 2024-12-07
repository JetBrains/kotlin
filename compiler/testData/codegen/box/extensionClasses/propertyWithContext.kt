// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-53706

fun box(): String {
    return with(SomeContext()) {
        "error".foo
    }
}

class SomeContext {
    val value: String = "OK"
}

context(SomeContext)
val String.foo: String
    get() = value
