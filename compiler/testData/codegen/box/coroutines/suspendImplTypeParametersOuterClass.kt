// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK

class Outer<U> {
    inner abstract class AbstractPersistence<T> {
        open suspend fun fetch(identifier: U): T? = null
    }

    fun foo(): String = AbstractPersistence::class.java.declaredMethods
        .single { it.name.contains("\$suspendImpl") }
        .toGenericString()
}

fun box(): String {
    val genericString = Outer<Unit>().foo()
    if (!genericString.startsWith("static <T,U>")) {
        return genericString
    }

    return "OK"
}