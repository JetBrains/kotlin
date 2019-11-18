// IGNORE_BACKEND_FIR: JVM_IR
package test

public interface FunDependencyEdge {
    val from: FunctionNode
}

public interface FunctionNode

public class FunctionNodeImpl : FunctionNode

class FunDependencyEdgeImpl(override val from: FunctionNodeImpl): FunDependencyEdge {
}

fun box(): String {
    (FunDependencyEdgeImpl(FunctionNodeImpl()) as FunDependencyEdge).from
    return "OK"
}
