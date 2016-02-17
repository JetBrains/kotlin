// "Specify type explicitly" "false"
// ERROR: This property must either have a type annotation, be initialized or be delegated

class A {
    val a
        get() = a
}