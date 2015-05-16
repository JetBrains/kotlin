// IS_APPLICABLE: false
<caret>@deprecated("")
fun foo() {
    bar() ?: return
}

fun bar(): String? = null