// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

@JsExport
data class Person(val id: Int, val name: String?)

@JsExport
val person = Person(42, null)

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
val <!SYNTAX!>(id, name)<!> = person

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
<!SYNTAX!>(val fullId = id, var fullName = name)<!> = person

@JsExport
fun main() {
    val (id, name) = person

    (val fullId = id, var fullName = name) = person
}
