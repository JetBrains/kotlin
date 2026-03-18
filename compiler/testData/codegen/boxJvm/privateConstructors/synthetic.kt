// TARGET_BACKEND: JVM

// WITH_STDLIB

// private constructors are transformed into synthetic
class PrivateConstructor private constructor() {
    class Nested { val a = PrivateConstructor() }
}

fun check(klass: Class<*>) {
    var hasSynthetic = false
    var hasSimple = false
    for (method in klass.getDeclaredConstructors()) {
        if (method.isSynthetic()) {
            hasSynthetic = true
        }
        else {
            hasSimple = true
        }
    }
    if (hasSynthetic && hasSimple) return
    throw AssertionError("Class should have both synthetic and non-synthetic constructor: ($hasSynthetic, $hasSimple)")
}

fun box(): String {
    check(PrivateConstructor::class.java)
    // Also check that synthetic accessors really work
    PrivateConstructor.Nested()
    return "OK"
}
