// "Implement members" "true"
// WITH_RUNTIME
abstract class A {
    abstract fun foo()
}

<caret>class B : A() {
}
