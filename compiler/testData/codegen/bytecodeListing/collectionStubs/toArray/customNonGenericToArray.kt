class InternalToArray(d: Collection<Any>): Collection<Any> by d {
    internal fun toArray(): Array<Int> = null!!
}

class PrivateToArray(d: Collection<Any>): Collection<Any> by d {
    private fun toArray(): Array<Int> = null!!
}

class PublicToArray(d: Collection<Any>): Collection<Any> by d {
    public fun toArray(): Array<Int> = null!!
}
