annotation class my
annotation class my1(val i : Int)
annotation class my2(val i : Int = 0)

@my fun foo() {}
<!INAPPLICABLE_CANDIDATE!>@my1<!> fun foo2() {}
@my1(2) fun foo3() {}
@my2() fun foo4() {}
@my2 fun foo41() {}
@my2(2) fun foo42() {}
