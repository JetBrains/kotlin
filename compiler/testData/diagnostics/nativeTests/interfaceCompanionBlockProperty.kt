// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions

interface I {
    companion {
        val foo = 1
        const val bar = 2
        var baz = 3
        val qux by lazy { 4 }
    }
}
