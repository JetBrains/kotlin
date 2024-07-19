// ISSUE: KT-69170
fun <T1> layerConfig(config: LayerConfigurer<T1>.() -> Unit) {}

interface LayerConfigurer<T2>

fun <T3> LayerConfigurer<T3>.subConfig(params: ParamTypeConfigurer<T3>.() -> Unit) {}

interface ParamTypeConfigurer<T4> {
    fun paramTypeConfig(resolver: (T4) -> Unit)
}

class IntBox(val param: Int)

fun foo() {
    layerConfig {
        subConfig<IntBox> {
            paramTypeConfig {
                it.param
            }
        }
    }
}
