// RUN_PIPELINE_TILL: BACKEND
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

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral, nullableType,
typeAliasDeclaration, typeParameter, typeWithExtension */
