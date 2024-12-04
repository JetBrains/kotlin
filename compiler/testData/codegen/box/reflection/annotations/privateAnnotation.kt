// TARGET_BACKEND: JVM
// WITH_REFLECT

annotation private class Ann(val name: String)

class A {
    @Ann("OK")
    fun foo() {}
}

fun box(): String {
    val ann = A::class.members.single { it.name == "foo" }.annotations.single() as Ann
    return ann.name
}
