// RUN_PIPELINE_TILL: BACKEND
interface DailyAggregatedDoubleFactor

private fun DailyAggregatedDoubleFactor.aggregateBy(reduce: (Double, Double) -> Double): Map<String, Double> {
    return mutableMapOf<String, Double>()
}

fun DailyAggregatedDoubleFactor.aggregateMin(): Map<String, Double> = aggregateBy(::minOf)

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, functionalType,
interfaceDeclaration */
