// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_DEFAULT_MODE: enable

package test

interface I {
    fun f() = object {}
}

class C : I

fun box(): String {
    val t = C().f().javaClass

    val simpleName = t.simpleName
    if (simpleName != "f$1" && simpleName != "") return "Fail simpleName: $simpleName"

    val enclosing = t.enclosingClass.name
    if (enclosing != "test.I") return "Fail enclosing: $enclosing"

    val name = t.name
    if (name != "test.I\$f$1") return "Fail name: $name"

    return "OK"
}
