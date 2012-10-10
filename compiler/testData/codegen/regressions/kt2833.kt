package test

public trait FunDependencyEdge {
    val from: FunctionNode
}

public trait FunctionNode

public class FunctionNodeImpl : FunctionNode

class FunDependencyEdgeImpl(override val from: FunctionNodeImpl): FunDependencyEdge {
}

fun box(): String {
    (FunDependencyEdgeImpl(FunctionNodeImpl()) as FunDependencyEdge).from
    return "OK"
}
