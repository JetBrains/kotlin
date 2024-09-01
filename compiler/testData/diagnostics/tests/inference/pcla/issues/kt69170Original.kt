// FIR_IDENTICAL
// ISSUE: KT-69170
fun <LAYER : Any> layerConfig(
    config: LayerConfigurer<LAYER>.() -> Unit,
): LayerResult<LAYER> {
    return object : LayerResult<LAYER> {}
}

interface LayerResult<LAYER : Any>

interface LayerConfigurer<LAYER : Any>

inline fun <LAYER : Any, reified EXACT_LAYER : LAYER> LayerConfigurer<LAYER>.subConfig(
    params: ParamsConfigLambda<EXACT_LAYER>,
) {}

fun interface ParamsConfigLambda<EXACT_LAYER : Any> {
    fun ParamTypeConfigurer<EXACT_LAYER>.configureParams()
}

interface ParamTypeConfigurer<EXACT_LAYER : Any> {
    fun <T : Any> paramTypeConfig(resolver: ParamResolver<EXACT_LAYER, T>)
}

fun interface ParamResolver<EXACT_LAYER, PARAM> {
    fun resolve(LAYER: EXACT_LAYER): PARAM
}
data class ParametrizedLayer(val param: Int)

fun foo() {
    layerConfig {
        subConfig<ParametrizedLayer, ParametrizedLayer>(
            params = { paramTypeConfig<Int> { it.param } } // Unresolved reference 'param'.
        )
    }
}
