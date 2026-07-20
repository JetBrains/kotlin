// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions

interface I {
    companion {
        val foo = 1
        const val bar = 2
        <!INTERFACE_COMPANION_BLOCK_VAR!>var<!> baz = 3
        val qux by lazy { 4 }
    }
}
