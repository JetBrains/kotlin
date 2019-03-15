// "Change to constructor invocation" "false"
// ACTION: Introduce import alias
// ERROR: This type has a constructor, and thus must be initialized here
// ERROR: This type is sealed, so it can be inherited by only its own nested classes or objects

class My {
    sealed class A

    class B : A<caret>
}