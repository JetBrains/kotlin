// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
package one

class C {
    companion {
        fun blockFun(): Int = 1
        val blockVal: String = "block"
    }

    companion object {
        fun companionObjectFun(): Int = 2
        val companionObjectVal: String = "companion"
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: C.class[blockFun;blockVal;getBlockVal]

// DECLARATIONS_NO_LIGHT_ELEMENTS: C.class[blockFun;blockVal]
