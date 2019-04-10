// FIX: Replace with 'checkNotNull()' call
// WITH_RUNTIME
fun test(foo: Int?) {
    <caret>if (foo == null) throw IllegalStateException()
}