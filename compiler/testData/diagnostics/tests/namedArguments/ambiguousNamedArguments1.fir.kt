interface A {
    fun foo(x : Int)
}

interface B {
    fun foo(y : Int)
}

interface C : A, B
interface D : B, A

fun foo(x : C, y : D){
    x.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>x<!> = 0)
    x.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>y<!> = 0)
    y.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>x<!> = 0)
    y.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>y<!> = 0)
}

abstract class C1 : A, B
abstract class D1 : A, B

fun bar(x : C1, y : D1){
    x.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>x<!> = 0)
    x.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>y<!> = 0)
    y.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>x<!> = 0)
    y.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>y<!> = 0)
}
