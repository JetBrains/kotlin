// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NameBasedDestructuring
@file:JsExport

data class Person(val id: Int, val name: String?)

val person = Person(42, null)

<!NON_EXPORTABLE_TYPE!>val <!SYNTAX!>[first, second]<!> = person<!>

<!NON_EXPORTABLE_TYPE!><!SYNTAX!>[val id, var name]<!> = person<!>

fun main() {
    val [first, second] = person

    [val id, var name] = person
}
