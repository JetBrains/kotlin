// FIR_COMPARISON
package test

open class BaseClass {
    companion object {
        val publicVal = 10
        fun publicFun() {}

        protected val protectedVal = 30
        protected fun protectedFun() {}

        private val privateVal = 30
        private fun privateFun() {}
    }
}

fun test() {
    BaseClass.<caret>
}

// EXIST: publicVal
// EXIST: publicFun

// ABSENT: protectedVal
// ABSENT: protectedFun
// ABSENT: privateVal
// ABSENT: privateFun
