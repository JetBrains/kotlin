// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
@file:JsExport

data class Person(val id: Int, val name: String?)

val person = Person(42, null)

<!NON_EXPORTABLE_TYPE!>val <!SYNTAX!>(id, name)<!> = person<!>

<!NON_EXPORTABLE_TYPE!><!SYNTAX!>(val fullId = id, var fullName = name)<!> = person<!>

fun main() {
    val (id, name) = person

    (val fullId = id, var fullName = name) = person
}
