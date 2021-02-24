// Copy of IR test

object A
object B

interface IFoo {
    val A.foo: B get() = B
}

interface IInvoke {
    operator fun B.invoke() = 42
}

fun test(fooImpl: IFoo, invokeImpl: IInvoke) {
    with(A) {
        with(fooImpl) {
            foo
            with(invokeImpl) {
                foo()
            }
        }
    }
}