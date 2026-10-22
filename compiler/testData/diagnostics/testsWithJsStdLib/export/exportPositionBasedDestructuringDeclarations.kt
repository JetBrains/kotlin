// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NameBasedDestructuring

@JsExport
data class Person(val id: Int, val name: String?)

@JsExport
val person = Person(42, null)

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
val <!SYNTAX!>[first, second]<!> = person

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
<!SYNTAX!>[val id, var name]<!> = person

@JsExport
fun main() {
    val [first, second] = person

    [val id, var name] = person
}
