// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NameBasedDestructuring

@JsExport
data class Person(val id: Int, val name: String?)

@JsExport
val person = Person(42, null)

@JsExport
val <!SYNTAX!>[first, second]<!> = person

@JsExport
<!SYNTAX!>[val id, var name]<!> = person

@JsExport
fun main() {
    val [first, second] = person

    [val id, var name] = person
}
