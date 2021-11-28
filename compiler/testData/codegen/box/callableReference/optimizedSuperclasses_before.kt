// !API_VERSION: 1.3
// TARGET_BACKEND: JVM
// WITH_STDLIB

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
    check(A::memberFunction, "[getName, getOwner, getSignature, invoke, invoke]", "unbound function reference")
    check(A()::memberFunction, "[getName, getOwner, getSignature, invoke, invoke]", "bound function reference")

    check(::topLevelProperty, "[get, getName, getOwner, getSignature]", "unbound property reference 0")
    check(A::memberProperty, "[get, getName, getOwner, getSignature]", "unbound property reference 1")
    check(A()::memberProperty, "[get, getName, getOwner, getSignature]", "bound property reference 1")

    return "OK"
}
