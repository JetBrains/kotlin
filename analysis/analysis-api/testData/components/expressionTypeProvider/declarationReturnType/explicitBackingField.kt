// LANGUAGE: +ExplicitBackingFields
// WITH_STDLIB
class A {
    val items: List<String>
        field = mutableListOf()
}
