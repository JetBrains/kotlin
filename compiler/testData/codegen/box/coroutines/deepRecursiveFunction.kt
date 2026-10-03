// WITH_STDLIB
// Each recursion level of DeepRecursiveFunction is started by `startCoroutineUninterceptedOrReturn`
// with the continuation of the calling level as completion.

class Tree(val left: Tree?, val right: Tree?)

fun deepTree(depth: Int): Tree? = (1..depth).fold(null as Tree?) { acc, _ -> Tree(acc, null) }

val treeDepth = DeepRecursiveFunction<Tree?, Int> { t ->
    if (t == null) 0 else maxOf(callRecursive(t.left), callRecursive(t.right)) + 1
}

// Mutual recursion between two functions goes through the cross-function trampoline.
val isEven: DeepRecursiveFunction<Int, Boolean> = DeepRecursiveFunction { n ->
    if (n == 0) true else isOdd.callRecursive(n - 1)
}

val isOdd: DeepRecursiveFunction<Int, Boolean> = DeepRecursiveFunction { n ->
    if (n == 0) false else isEven.callRecursive(n - 1)
}

fun box(): String {
    val depth = treeDepth(deepTree(1000))
    if (depth != 1000) return "fail 1: $depth"
    if (!isEven(1000)) return "fail 2"
    if (isOdd(1000)) return "fail 3"
    return "OK"
}
