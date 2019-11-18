// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

interface A {
    fun foo(): Collection<Any>
}

abstract class B : A {
    override fun foo(): Collection<String> = null!!
}

fun box(): String {
    val clazz = B::class.java
    if (clazz.declaredMethods.first().genericReturnType.toString() != "java.util.Collection<java.lang.String>") return "fail 1"

    if (clazz.methods.filter { it.name == "foo" }.size != 1) return "fail 2"

    return "OK"
}
