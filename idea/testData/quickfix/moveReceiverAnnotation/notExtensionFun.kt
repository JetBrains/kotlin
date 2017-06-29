// "Move annotation to receiver type" "false"
// ERROR: '@receiver:' annotations can only be applied to the receiver type of extension function or extension property declarations
// ACTION: Convert to expression body
// ACTION: Make internal
// ACTION: Make private

annotation class Ann

@receiver:Ann<caret>
fun foo() {
}