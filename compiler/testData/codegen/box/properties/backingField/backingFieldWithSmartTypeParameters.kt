// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ExplicitBackingFields

// WITH_STDLIB

val items: List<String>
    field = mutableListOf()

fun box(): String {
    items.add("OK")
    return items.last()
}
