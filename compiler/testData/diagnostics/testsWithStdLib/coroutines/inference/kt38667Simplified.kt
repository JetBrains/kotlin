// FIR_IDENTICAL
// ISSUE: KT-38667

interface PropKey<T>
abstract class AnimationBuilder<T>
class TransitionSpec<S> {
    fun <E> tween(init: AnimationBuilder<E>.() -> Unit): AnimationBuilder<E> = TODO()
    fun <F> PropKey<F>.using(builder: AnimationBuilder<F>) {}
}
class TransitionDefinition<X> {
    fun transition(fromState: X, init: TransitionSpec<X>.() -> Unit) {}
}

fun <T> transitionDefinition(init: TransitionDefinition<T>.() -> Unit) {}

fun main(intProp: PropKey<Int>, w: Int) {
    val defn = transitionDefinition {
        transition(w) {
            intProp.using(tween {})
        }
    }
}
