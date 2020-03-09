// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

class A {
    fun memberFunction() {}
    val memberProperty: String = ""
}

val topLevelProperty: Int = 0

fun check(reference: Any, expected: String, message: String) {
    val actual = reference.javaClass.declaredMethods.map { it.name }.sorted().toString()
    if (expected != actual) {
        throw AssertionError("Fail on $message. Expected: $expected. Actual: $actual")
    }
}

fun box(): String {
    check(A::memberFunction, "[invoke, invoke]", "unbound function reference")
    check(A()::memberFunction, "[invoke, invoke]", "bound function reference")

    check(::topLevelProperty, "[get]", "unbound property reference 0")
    check(A::memberProperty, "[get]", "unbound property reference 1")
    check(A()::memberProperty, "[get]", "bound property reference 1")

    return "OK"
}
