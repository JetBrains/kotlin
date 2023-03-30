open class A(x: () -> Unit)

class B : A {
    constructor(i: Int) : super(
        {
            foo(i)
        }
    )

    constructor(l: Long) : super(
        {
            foo(l)
        }
    )
}

fun foo(any: Any) {}