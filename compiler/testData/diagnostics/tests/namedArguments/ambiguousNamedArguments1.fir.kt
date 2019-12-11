interface A {
    fun foo(x : Int)
}

interface B {
    fun foo(y : Int)
}

interface C : A, B
interface D : B, A

fun foo(x : C, y : D){
    x.foo(x = 0)
    x.<!INAPPLICABLE_CANDIDATE!>foo<!>(y = 0)
    y.<!INAPPLICABLE_CANDIDATE!>foo<!>(x = 0)
    y.foo(y = 0)
}

abstract class C1 : A, B
abstract class D1 : A, B

fun bar(x : C1, y : D1){
    x.foo(x = 0)
    x.<!INAPPLICABLE_CANDIDATE!>foo<!>(y = 0)
    y.foo(x = 0)
    y.<!INAPPLICABLE_CANDIDATE!>foo<!>(y = 0)
}