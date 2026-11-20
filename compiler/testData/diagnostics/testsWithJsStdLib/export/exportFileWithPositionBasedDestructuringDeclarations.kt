// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NameBasedDestructuring
@file:JsExport

data class Person(val id: Int, val name: String?)

val person = Person(42, null)

val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>[first, second]<!> = person

<!NON_LOCAL_DESTRUCTURING_DECLARATION!>[val id, var name]<!> = person

fun main() {
    val [first, second] = person

    [val id, var name] = person
}
