// TARGET_BACKEND: JVM
// WITH_RUNTIME

// In the old JVM backend, FieldOwnerContext is sensitive to the order of properties which it invents name for. Companion object properties
// are usually the first, so A.Companion.x here gets the name "x". After that it tries to invent a new name for A.x but fails because
// @JvmField-annotated properties cannot be renamed, which leads to a JVM declaration clash error.
// IGNORE_BACKEND: JVM

class A {
    @JvmField
    val x = "outer"

    companion object {
        val x = "companion"
    }
}

fun box(): String {
    if (A().x != "outer") return "Fail outer"
    if (A.x != "companion") return "Fail companion"

    return "OK"
}
