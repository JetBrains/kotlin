// FIR_IDENTICAL
// ISSUE: KT-61075
// WITH_REFLECT

import kotlin.properties.ReadWriteProperty

open class BaseState
interface CustomComparable<T>
annotation class XCollection

fun <E> treeSet(): ReadWriteProperty<Any?, E> where E: BaseState, E: CustomComparable<E> = TODO()

internal class VisibleTreeState {
    internal class State: BaseState(), CustomComparable<State>

    // K1: ok
    // K2: NEW_INFERENCE_ERROR
    @get:XCollection var expandedNodes: State by treeSet()

    // K1: ok
    // K2: NEW_INFERENCE_ERROR
    @set:XCollection var selectedNodes: State by treeSet()
}