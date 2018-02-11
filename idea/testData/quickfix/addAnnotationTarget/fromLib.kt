// "Add annotation target" "false"
// WITH_RUNTIME
// ACTION: Make internal
// ACTION: Make private
// ACTION: Specify type explicitly
// ERROR: This annotation is not applicable to target 'top level property without backing field or delegate'

<caret>@JvmField
val x get() = 42
