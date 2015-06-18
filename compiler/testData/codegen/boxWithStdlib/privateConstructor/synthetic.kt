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
    check(javaClass<PrivateConstructor>())
    // Also check that synthetic accessors really work
    PrivateConstructor.Nested()
    return "OK"
}
