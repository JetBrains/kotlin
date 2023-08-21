// FIR_IDENTICAL
// ISSUE: KT-38667
// !OPT_IN: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION

import kotlin.experimental.ExperimentalTypeInference

abstract class AnimationVector
class AnimationVector1D : AnimationVector()
interface PropKey<T, V : AnimationVector>
class IntPropKey : PropKey<Int, AnimationVector1D>
abstract class AnimationBuilder<T>
abstract class DurationBasedAnimationBuilder<T> : AnimationBuilder<T>()
class TweenBuilder<T> : DurationBasedAnimationBuilder<T>()
class TransitionSpec<S> {
    fun <E> tween(init: TweenBuilder<E>.() -> Unit): DurationBasedAnimationBuilder<E> = TweenBuilder<E>().apply(init)
    infix fun <F, V : AnimationVector> PropKey<F, V>.using(builder: AnimationBuilder<F>) {}
}
class TransitionDefinition<X> {
    fun transition(fromState: X? = null, toState: X? = null, init: TransitionSpec<X>.() -> Unit) {}
}
@OptIn(ExperimentalTypeInference::class)
fun <T> transitionDefinition(init: TransitionDefinition<T>.() -> Unit) = TransitionDefinition<T>().apply(init)

fun main() {
    val intProp = IntPropKey()
    val defn = transitionDefinition {
        transition(1, 2) {
            intProp using tween {

            }
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("TransitionDefinition<kotlin.Int>")!>defn<!>
}
