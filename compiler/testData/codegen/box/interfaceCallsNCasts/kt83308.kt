// ISSUE: KT-83308, fixed in 2.4.0-Beta1
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3

interface Node

internal class NodeImpl : Node

interface BsonNode : Node {
    override fun toString(): String
}

abstract class AbstractBsonNode private constructor(
    private val node: NodeImpl,
) : Node by node, BsonNode {

    constructor() : this(NodeImpl())

    final override fun toString(): String = "OK"
}

interface CompoundNode<N : Node>

interface CompoundBsonNode : BsonNode, CompoundNode<BsonNode>

abstract class AbstractCompoundBsonNode() : AbstractBsonNode(), CompoundBsonNode

//------

interface ValueOperators

interface ValueAccumulators<From : Any, Into : Any> : ValueOperators, CompoundBsonNode

interface ArithmeticValueAccumulators<From : Any, Into : Any> : ValueAccumulators<From, Into>

interface AggregationOperators : ValueOperators

interface AccumulationOperators<From : Any, Into : Any> : ValueAccumulators<From, Into>,
    AggregationOperators,
    ArithmeticValueAccumulators<From, Into>

internal class AccumulationOperatorsImpl<From : Any, Into : Any>(
) : AbstractCompoundBsonNode(),
    AccumulationOperators<From, Into>

//------

internal fun <Document : Any, Out : Any> group() = AccumulationOperatorsImpl<Document, Out>()

fun box(): String {
    return group<String, String>().toString()
}