// SCOPE_DUMP: Data:equals, Data:hashCode, Data:toString

interface AnyNeighbor {
    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    override fun toString(): String
}

class Impl : AnyNeighbor {
    override fun equals(other: Any?): Boolean {
        return true
    }

    override fun hashCode(): Int {
        return 0
    }

    override fun toString(): String {
        return ""
    }
}

data class Data(val i: Impl) : AnyNeighbor by i
