// "Create type parameter in interface 'List'" "false"
// ACTION: Introduce import alias
// ERROR: One type argument expected for interface List<out E>
// WITH_RUNTIME
fun foo(): List<String, String<caret>> {
    return listOf(1)
}