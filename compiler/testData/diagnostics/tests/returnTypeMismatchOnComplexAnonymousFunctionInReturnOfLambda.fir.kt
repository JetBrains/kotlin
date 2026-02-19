// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78942
// RENDER_DIAGNOSTICS_FULL_TEXT
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun <T> runLike(block: () -> T): T = block()

fun returnNullableString(): String? = null
fun returnString(): String = ""

fun test() {
    var str: String <!INITIALIZER_TYPE_MISMATCH!>=<!> runLike {
        <!RETURN_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>fun(): String {
            if (true) return <!RETURN_TYPE_MISMATCH!>returnNullableString()<!>
            return ""
        }<!>
    }
    str <!ASSIGNMENT_TYPE_MISMATCH!>=<!> runLike {
        <!RETURN_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>fun(): String {
            if (true) return returnString()
            return ""
        }<!>
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionalType, ifExpression, lambdaLiteral,
nullableType, typeParameter */
