// !DIAGNOSTICS: -UNUSED_PARAMETER
annotation class Ann1
annotation class Ann2(val x: Int)

class A {
    @Ann1
    constructor()
    <!INAPPLICABLE_CANDIDATE!>@Ann2<!>
    constructor(x1: Int)
    @Ann2(2)
    constructor(x1: Int, x2: Int)
}
