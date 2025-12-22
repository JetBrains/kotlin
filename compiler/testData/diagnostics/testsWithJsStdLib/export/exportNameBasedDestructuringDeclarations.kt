// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

data class Person(val id: Int, val name: String?)

val person = Person(42, null)

@JsExport
val <!SYNTAX!>(id, name)<!> = person

@JsExport
<!SYNTAX!>(val fullId = id, var fullName = name)<!> = person

@JsExport
fun main() {
    val (id, name) = person

    (val fullId = id, var fullName = name) = person
}
