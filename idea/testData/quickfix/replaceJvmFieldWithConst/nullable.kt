// "Replace '@JvmField' with 'const'" "false"
// WITH_RUNTIME
// ERROR: JvmField has no effect on a private property
// ACTION: Remove explicit type specification
<caret>@JvmField private val number: Int? = 42