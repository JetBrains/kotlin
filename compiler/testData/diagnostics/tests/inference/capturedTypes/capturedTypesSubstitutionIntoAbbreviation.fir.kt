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

fun main() {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Nothing, kotlin.Unit>")!>getTag().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Nothing, kotlin.Unit>")!>getTag2().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Nothing, kotlin.Unit>")!>getTag3().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Nothing, kotlin.Unit>")!>getTag4().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Nothing, kotlin.Any?>")!>getTag5().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Nothing, kotlin.Unit>")!>getTag6().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Nothing, kotlin.Unit>")!>getTag7().action<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Nothing, kotlin.Any?>")!>getTag8().action<!>
}
