// "Replace '@JvmField' with 'const'" "false"
// WITH_RUNTIME
// ERROR: JvmField has no effect on a private property
// ACTION: Remove explicit type specification
fun getText() = ""
<caret>@JvmField private val text: String = getText()