// "Replace '@JvmField' with 'const'" "false"
// WITH_RUNTIME
// ERROR: JvmField has no effect on a private property
// ACTION: Add 'const' modifier
// ACTION: Specify type explicitly
val three = 3
<caret>@JvmField private val text = "${2 + three}"