// FIR_IDENTICAL

interface FlameGraphModel<T>

internal typealias CallUsageNode = CallTreeNode<BaseCallStackElement>

interface CallTreeNode<out T : Any> : TreeNodeWithParent<CallWithValue<T>>

interface TreeNodeWithParent<out Data>

interface CallWithValue<out T : Any>

abstract class BaseCallStackElement

open class CallUsageNodeFlameGraphModel<Call : Any> : FlameGraphModel<CallTreeNode<Call>>

fun foo(model: FlameGraphModel<CallUsageNode>) {
    // K1: Ok
    val afterCast = model as CallUsageNodeFlameGraphModel
}

internal typealias CallTreeNodeTypealias<K> = CallTreeNode<K>

open class CallUsageNodeFlameGraphModelWithTypealiasedSupertypeArgument<Call : Any> : FlameGraphModel<CallTreeNodeTypealias<Call>>

fun bar(model: FlameGraphModel<CallUsageNode>) {
    // K1: Ok
    val afterCast = model as CallUsageNodeFlameGraphModelWithTypealiasedSupertypeArgument
}
