// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// WITH_STDLIB

val items: List<String>
    field = mutableListOf()

fun box(): String {
    items.add("OK")
    return items.last()
}
