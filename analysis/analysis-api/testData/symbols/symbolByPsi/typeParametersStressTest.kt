// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

fun <A : Number> topLevelFunction() {}
val <B> B.topLevelProperty: B get() = this

class TopLevelClass<C, D : C> {
    constructor(arg: D)

    class NestedClass<E> {
        fun <F> nestedFunction() {
            fun <G> localFunction() {
                class LocalClass<H> {
                    fun <I> localMemberFunction() {}
                    inner class LocalInnerClass<J> {

                    }
                }
            }
        }
    }

    inner class InnerClass<K> {
        fun <L> nestedFunction() {
            fun <M> localFunction() {
                class LocalClass<N> {
                    fun <O> localMemberFunction() {}
                    inner class LocalInnerClass<P> {

                    }
                }
            }

            val localProperty = 1
        }
    }
}

typealias TopLevelTypeAlias<Q> = Int