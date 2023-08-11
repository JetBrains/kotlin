// ISSUE: KT-61067
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun main() {
    val storages: HashMap<String, String> = HashMap<String, String>()
    val a = <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>storages<!NO_SET_METHOD!>["4"]<!> = ""<!> //K1 compile error - Kotlin: Assignments are not expressions, and only expressions are allowed in this context
    storages<!NO_SET_METHOD!>["4"]<!> = ""

    var nonStorages: Int = 10
    val b = <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>nonStorages = 20<!>
    nonStorages = 20
}
