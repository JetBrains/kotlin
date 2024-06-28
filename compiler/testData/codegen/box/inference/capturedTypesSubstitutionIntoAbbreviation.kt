// JVM_ABI_K1_K2_DIFF: KT-63858

typealias Action<K> = (@UnsafeVariance K) -> Unit
typealias Action2<K> = (@UnsafeVariance K) -> K

data class Tag<L>(val action: Action<L>)
data class Tag2<L>(val action: Action<in L>)
data class Tag3<in L>(val action: Action<L>)
data class Tag4<in L>(val action: Action<in L>)
data class Tag5<L>(val action: Action2<L>)
data class Tag6<out L>(val action: Action<in L>)
data class Tag7<out L>(val action: Action<L>)
data class Tag8<out L>(val action: Action2<L>)

fun getTag(): Tag<*> = Tag<Int> {}
fun getTag2(): Tag2<*> = Tag2<Int> {}
fun getTag3(): Tag3<*> = Tag3<Int> {}
fun getTag4(): Tag4<*> = Tag4<Int> {}
fun getTag5(): Tag5<*> = Tag5<Int> { 1 }
fun getTag6(): Tag6<*> = Tag6<Int> { }
fun getTag7(): Tag7<*> = Tag7<Int> { }
fun getTag8(): Tag8<*> = Tag8<Int> { 1 }

fun box(): String {
    getTag().action
    getTag2().action
    getTag3().action
    getTag4().action
    getTag5().action
    getTag6().action
    getTag7().action
    getTag8().action
    return "OK"
}