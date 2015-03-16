// "Change 'foo' function return type to 'Any'" "true"
fun foo() {
    class A

    return <caret>A()
}