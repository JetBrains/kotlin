import kotlin.contracts.*

class FirAnnotationArgumentMappingBuilder {
    val mapping: MutableMap<String, String> = mutableMapOf()

    fun build(): FirAnnotationArgumentMapping {
        return FirAnnotationArgumentMapping(mapping)
    }
}

@OptIn(ExperimentalContracts::class)
fun buildAnnotationArgumentMapping(init: FirAnnotationArgumentMappingBuilder.() -> Unit): FirAnnotationArgumentMapping {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirAnnotationArgumentMappingBuilder().apply(init).build()
}

class FirAnnotationArgumentMapping(mapping: Map<String, String>)

class ValueParameter(val name: String)
class Argument(val name: String)

fun createArgumentMapping(
    valueParameters: List<ValueParameter>?,
    arguments: List<Argument>
): FirAnnotationArgumentMapping {
    return buildAnnotationArgumentMapping build@{
        val parameterByName: Map<String, ValueParameter>? by lazy {
            val valueParameters = valueParameters ?: return@lazy null
            valueParameters.associateBy { it.name }
        }

        arguments.mapNotNull {
            val name = it.name
            val value = parameterByName?.get(name)?.name ?: return@mapNotNull null
            name to value
        }.toMap(mapping)
    }
}
