// "Wrap with '?.let { ... }' call" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Convert to run
// ACTION: Convert to with
// ACTION: Replace with safe (?.) call
// ACTION: Surround with null check
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type B?
// WITH_RUNTIME

class A {
    fun foo() {}
}
class B(val a: A)
fun test(b: B?) {
    b<caret>.a.foo()  // b.a is UNSAFE_CALL
}