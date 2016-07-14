// "Create member function 'A.foo'" "true"

class A

fun test() {
    println("a = ${A().<caret>foo()}")
}