// "Move annotation to receiver type" "false"
// ERROR: This annotation is not applicable to target 'top level property without backing field or delegate' and use site target '@receiver'
// ACTION: Make internal
// ACTION: Make private
// ACTION: Specify type explicitly

annotation class Ann

@receiver:Ann<caret>
val bar get() = ""