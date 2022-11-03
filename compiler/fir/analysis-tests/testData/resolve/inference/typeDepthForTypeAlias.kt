// FIR_DISABLE_LAZY_RESOLVE_CHECKS
typealias SymbolToTransformer = MutableMap<Int, (String) -> Double>


fun SymbolToTransformer.add() {}

fun foo(
    symbolToTransformer: SymbolToTransformer
) {
    symbolToTransformer.myApply {
        add()
    }
}

fun <T> T.myApply(x: T.() -> Unit) {}
