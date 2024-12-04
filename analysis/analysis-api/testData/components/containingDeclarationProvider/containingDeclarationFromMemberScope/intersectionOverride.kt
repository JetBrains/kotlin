interface A {
    fun <T>foo()
}

interface B {
    fun <T>foo() {}
}

>class Y<caret> : A, B