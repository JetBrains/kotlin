// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
package cba

class WithCompanionBlock {
    companion {
        fun blockFun(): Int = 1
        val blockVal: String = "block"
    }
}

class WithCompanionBlockAndObject {
    companion {
        fun blockFun(): Int = 1
    }

    companion object {
        fun companionObjectFun(): Int = 2
    }
}
