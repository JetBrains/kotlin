class InternalGenericToArray<T>(d: Collection<T>): Collection<T> by d {
    internal fun <T> toArray(arr: Array<T>): Array<T> = null!!
}