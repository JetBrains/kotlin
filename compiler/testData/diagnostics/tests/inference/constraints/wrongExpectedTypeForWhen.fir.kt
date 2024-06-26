fun foo() {
    var f: Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Int; kotlin.Function1<kotlin.Long, kotlin.Unit>")!>if (true) <!ARGUMENT_TYPE_MISMATCH!>{ x: Long ->  }<!> else <!ARGUMENT_TYPE_MISMATCH!>{ x: Long ->  }<!><!>
}

class A {
    var x: Int
        get(): Int = <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.Function0<kotlin.Int>")!>if (true) { <!ARGUMENT_TYPE_MISMATCH!>{42}<!> } else { <!ARGUMENT_TYPE_MISMATCH!>{24}<!> }<!>
        set(i: Int) {}
}
