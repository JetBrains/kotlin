// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
package one

class C {
    companion {
        public fun publicFun(): Int = 1
        internal fun internalFun(): Int = 2
        private fun privateFun(): Int = 3

        public val publicVal: String = "p"
        internal val internalVal: String = "i"
        private val privateVal: String = "pr"
    }
}
