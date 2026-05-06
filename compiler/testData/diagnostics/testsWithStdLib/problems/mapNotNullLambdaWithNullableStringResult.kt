// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// DUMP_INFERENCE_LOGS: FIXATION
// WITH_STDLIB

fun getMetrics(packageDependenciesData: List<String>) {
    val urls = packageDependenciesData.mapNotNull {
        val url = it.removeSuffix(".git")

        when {
            url.startsWith("https://") -> url.removePrefix("https://")
            else -> null.also { println("Unknown dependency URL type: $url") }
        }
    }

    for (it in urls) {
        PROJECT_DEPENDENCY.metric(it)
    }
}

inline fun <N, R : Any> Iterable<N>.mapNotNull(transform: (N) -> R?): List<R> =
    mapNotNullTo(ArrayList<R>(), transform)

val PROJECT_DEPENDENCY = EventId1<String?>()

class EventId1<in V> {
    fun metric(value1: V) {}
}

/* GENERATED_FIR_TAGS: capturedType, enumDeclaration, enumEntry, functionDeclaration, starProjection, stringLiteral,
whenExpression */
