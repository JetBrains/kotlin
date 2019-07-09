// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun box(): String {
    Boolean::class.java
    Byte::class.java
    Short::class.java
    Char::class.java
    Int::class.java
    Long::class.java
    Float::class.java
    Double::class.java
    return "OK"
}
