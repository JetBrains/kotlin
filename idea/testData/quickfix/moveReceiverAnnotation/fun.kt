// "Move annotation to receiver type" "true"

annotation class Ann

@receiver:Ann<caret>
fun String.foo() {
}