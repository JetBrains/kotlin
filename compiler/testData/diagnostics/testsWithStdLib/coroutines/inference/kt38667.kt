// FIR_IDENTICAL
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
    fun <T> tween(init: TweenBuilder<T>.() -> Unit): DurationBasedAnimationBuilder<T> = TweenBuilder<T>().apply(init)
    infix fun <T, V : AnimationVector> PropKey<T, V>.using(builder: AnimationBuilder<T>) {}
}
class TransitionDefinition<T> {
    fun transition(fromState: T? = null, toState: T? = null, init: TransitionSpec<T>.() -> Unit) {}
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
