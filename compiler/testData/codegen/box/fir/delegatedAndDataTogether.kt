// TARGET_BACKEND: JVM_IR
// ISSUE: KT-58926

fun box(): String {
    val i1 = Impl()
    val i2 = Impl()
    val d1 = Data(i1, 1)
    val d2 = Data(i2, 2)
    return if (d1 != d2) "FAIL: should be equal" else "OK"
}

interface AnyNeighbor {
    override fun equals(other: Any?): Boolean
}

class Impl : AnyNeighbor {
    override fun equals(other: Any?): Boolean = true
}

data class Data(val i: Impl, val j: Int) : AnyNeighbor by i
