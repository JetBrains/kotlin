// WITH_RUNTIME
<caret>@deprecated("")
fun foo(s: String): String {
    return s.substring(1) + Int.MAX_VALUE
}
