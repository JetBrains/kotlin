// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK

abstract class AbstractPersistence<T, U> {
    open suspend fun fetch(identifier: U): T? = null
}

fun box(): String {
    val genericString =
        AbstractPersistence::class.java.declaredMethods
            .single { it.name.contains("\$suspendImpl") }
            .toGenericString()

    if (!genericString.startsWith("static <T,U>")) {
        return genericString
    }

    return "OK"
}
