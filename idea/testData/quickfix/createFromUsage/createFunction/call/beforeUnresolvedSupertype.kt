// "Create function 'foo' from usage" "true"
// ERROR: Unresolved reference: B

class A: B {

}

fun test() {
    A().<caret>foo()
}