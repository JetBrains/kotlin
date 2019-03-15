// WITH_RUNTIME
class Out<out T>
class In<in R>
open class Open

@JvmSuppressWildcards(true)
fun deepOpen(x: Out<Out<Out<Open>>>) {}
// method: OnFunctionKt::deepOpen
// generic signature: (LOut<LOut<LOut<LOpen;>;>;>;)V

interface A<T> {
    @JvmSuppressWildcards(true)
    fun foo(): Out<T>
}
// method: A::foo
// generic signature: ()LOut<TT;>;

interface B {
    @JvmSuppressWildcards(true)
    fun foo(): In<Open>
}
// method: B::foo
// generic signature: ()LIn<LOpen;>;

@JvmSuppressWildcards(false)
fun bar(): Out<Open> = null!!
// method: OnFunctionKt::bar
// generic signature: ()LOut<+LOpen;>;
