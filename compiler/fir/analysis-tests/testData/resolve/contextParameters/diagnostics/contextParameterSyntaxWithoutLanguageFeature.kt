// RUN_PIPELINE_TILL: FRONTEND

<!UNSUPPORTED_FEATURE!>context(s: String)<!>
fun foo() {}

fun bar(
    x: List<<!UNSUPPORTED_FEATURE!>context(String)<!> () -> Unit>
) {}
