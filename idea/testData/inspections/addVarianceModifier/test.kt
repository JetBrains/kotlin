interface EffectivelyOut<T> {
    fun foo(): T
    val bar: T
}
interface EffectivelyIn<T> {
    fun foo(arg: T)
}
interface Invariant1<T> {
    var bar: T
}
interface Invariant2<T> {
    fun T.foo(): T
}
interface Invariant3<T : Invariant1<T>> {
    fun T.foo()
}
abstract class AbstractOut<T> {
    abstract val foo: T
    private var bar = foo
}
abstract class AbstractIn<T>(private val foo: T) {
    fun bar(arg: T) = foo == arg
}
interface Empty<T> // here we do not report anything to avoid ambiguity

abstract class AbstractInv<T>(var foo: T)

class InvUser<T>(val user: AbstractInv<T>)