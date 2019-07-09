// "Change to constructor invocation" "false"
// ACTION: Introduce import alias
// ERROR: This type has a constructor, and thus must be initialized here
// ERROR: This type is sealed, so it can be inherited by only its own nested classes or objects
sealed class A

fun test() {
    class B : A<caret>
}