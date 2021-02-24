// "Change to constructor invocation" "true"
// ACTION: Introduce import alias

class My {
    sealed class A

    class B : A<caret>
}
