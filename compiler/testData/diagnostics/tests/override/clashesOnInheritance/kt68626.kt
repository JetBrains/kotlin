// ISSUE: KT-68626
interface MyType<T : Comparable<T>>

class MyTypeImpl<T : Comparable<T>> : MyType<T>

open class TestClass {
    operator fun String.invoke(other: String): String = this + other
    operator fun <T : MyType<*>> String.invoke(other: String): Number = TODO()
}

abstract class ParametrizedParent<S : ParametrizedParent<S>> : TestClass()
abstract class NonParametrizedParent : TestClass()

class OverloadsBroken : ParametrizedParent<OverloadsBroken>()
class OverloadsWork : NonParametrizedParent()
