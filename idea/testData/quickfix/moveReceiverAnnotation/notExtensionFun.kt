// "Move annotation to receiver type" "false"
// ERROR: This annotation is not applicable to target 'top level function' and use site target '@receiver'
// ACTION: Make internal
// ACTION: Make private

annotation class Ann

@receiver:Ann<caret>
fun foo() {
}