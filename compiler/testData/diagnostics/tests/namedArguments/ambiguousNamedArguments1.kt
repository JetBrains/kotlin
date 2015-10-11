interface A {
    fun foo(x : Int)
}

interface B {
    fun foo(y : Int)
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface C<!> : A, B
<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface D<!> : B, A

fun foo(x : C, y : D){
    x.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>y<!> = 0)
    y.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>x<!> = 0)
}