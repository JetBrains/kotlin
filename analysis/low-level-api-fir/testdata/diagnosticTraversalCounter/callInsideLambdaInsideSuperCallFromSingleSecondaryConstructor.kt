open class A(x: () -> Unit)

class B : A {
    constructor(i: Int) : super(
        {
            foo(i)
        }
    )
}

fun foo(any: Any) {}