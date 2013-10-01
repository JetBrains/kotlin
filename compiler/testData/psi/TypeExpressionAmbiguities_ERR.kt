fun foo() {
    foo<Int?>()
    fooo<Double?addddd>()
    dd<Triple<Int, Int, Int>>(if (true) Triple(1, 1, 1) else Triple(2, 2, 2))
    foo(bar<a, b, c>(d))
    foo(bar<a, b+1, c>(d))
    foo(bar<a, f(), c>(d))
    foo(bar<a, a : b, c>(d))
    a : b < c
    d as Foo < bar
    x is Foo < bar
}
