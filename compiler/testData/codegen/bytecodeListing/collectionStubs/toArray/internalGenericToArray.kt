// JVM_ABI_K1_K2_DIFF: KT-63828
class InternalGenericToArray<T>(d: Collection<T>): Collection<T> by d {
    internal fun <T> toArray(arr: Array<T>): Array<T> = null!!
}