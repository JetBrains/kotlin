// TARGET_BACKEND: JVM
// WITH_RUNTIME
// !DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS
// IGNORE_BACKEND_FIR: JVM_IR

fun <E : Enum<E>> createMap(enumClass: Class<E>) {}

enum class A

fun box(): String {
    val enumClass: Class<Enum<*>> = A::class.java as Class<Enum<*>>
    createMap(enumClass)
    return "OK"
}
