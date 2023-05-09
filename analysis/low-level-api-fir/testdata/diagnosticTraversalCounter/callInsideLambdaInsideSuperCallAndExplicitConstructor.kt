open class B(x: () -> Unit)

class A() : B(1, {
    foo()
})

fun foo() {}