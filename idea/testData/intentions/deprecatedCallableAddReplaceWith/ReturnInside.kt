// IS_APPLICABLE: false
<caret>@Deprecated("")
fun foo() {
    bar() ?: return
}

fun bar(): String? = null