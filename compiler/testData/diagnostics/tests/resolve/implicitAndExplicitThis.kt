// !LANGUAGE: -NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// !WITH_NEW_INFERENCE

abstract class A<Context : A<Context>> {
    val x: X = TODO()

    fun getGetX(): X = TODO()
}

class B<D> : A<B<D>>()

class C : A<C>()

interface X {
    fun foo()
}

fun B<*>.checkValueArguments() {
    this.x.foo()
    x.foo()

    this.getGetX().foo()
    getGetX().foo()
}

fun C.test() {
    x.foo()
}