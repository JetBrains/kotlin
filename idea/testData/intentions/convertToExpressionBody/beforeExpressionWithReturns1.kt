// "class org.jetbrains.jet.plugin.intentions.ConvertToExpressionBodyAction" "false"

fun foo(p: Boolean): String {
    return bar() ?: return "a"
}

fun bar(): String? = null