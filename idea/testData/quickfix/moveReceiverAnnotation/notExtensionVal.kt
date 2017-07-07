// "Move annotation to receiver type" "false"
// ERROR: '@receiver:' annotations can only be applied to the receiver type of extension function or extension property declarations
// ACTION: Make internal
// ACTION: Make private
// ACTION: Specify type explicitly

annotation class Ann

@receiver:Ann<caret>
val bar get() = ""