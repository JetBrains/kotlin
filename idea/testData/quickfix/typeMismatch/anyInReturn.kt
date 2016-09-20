// "Change return type of current function 'foo' to 'Any'" "true"
fun foo() {
    class A

    return <caret>A()
}