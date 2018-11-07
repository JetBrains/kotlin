// "Add annotation target" "false"
// WITH_RUNTIME
// ACTION: Make internal
// ACTION: Specify type explicitly
// ACTION: Add use-site target 'property'
// ERROR: This annotation is not applicable to target 'top level property without backing field or delegate'

<caret>@JvmField
val x get() = 42
