// "Move annotation to receiver type" "false"
// ERROR: This annotation is not applicable to target 'declaration' and use site target '@receiver'
// ACTION: Make internal
// ACTION: Make private
// ACTION: Add annotation target

annotation class Ann

@receiver:Ann<caret>
fun foo() {
}