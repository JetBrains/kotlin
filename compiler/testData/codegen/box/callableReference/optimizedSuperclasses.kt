// TARGET_BACKEND: JVM
// WITH_STDLIB

class A {
    fun memberFunction() {}
    val memberProperty: String = ""
}

val topLevelProperty: Int = 0
fun A.extensionFunction() {}
val A.extensionProperty: String get() = ""

fun check(reference: Any, expected: String, message: String) {
    val actual = reference.javaClass.declaredMethods.map { it.name }.sorted().toString()
    if (expected != actual) {
        throw AssertionError("Fail on $message. Expected: $expected. Actual: $actual")
    }
}

fun box(): String {
    check(A::memberFunction, "[invoke, invoke]", "unbound function reference")
    check(A()::memberFunction, "[invoke, invoke]", "bound function reference")

    check(::topLevelProperty, "[get]", "unbound top-level property reference 0")
    check(A::memberProperty, "[get]", "unbound member property reference 1")
    check(A()::memberProperty, "[get]", "bound member property reference 1")
    check(A::extensionProperty, "[get]", "unbound extension property reference 1")
    check(A()::extensionProperty, "[get]", "bound extension property reference 1")

    return "OK"
}
