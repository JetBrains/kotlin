// "Change function signature to 'override fun f(y: S, x: List<Set<R>>)'" "true"
trait A<P,Q> {
    fun f(a: Q, b: List<Set<P>>)
}

class B<R,S> : A<R,S> {
    <caret>override fun f(x: List<Set<R>>, y: S) {}
}
