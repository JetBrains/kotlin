// "Create member function 'A.foo'" "true"
// ERROR: Unresolved reference: B

class A: B {

}

fun test() {
    A().<caret>foo()
}