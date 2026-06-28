// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
data class EvalDataDescription<In, Out>(
    val name: String,
    val description: String?,
    val problemIndicators: List<ProblemIndicator<Out>> = emptyList(),
)

typealias TrivialEvalData<T> = EvalDataDescription<T, T>

sealed interface ProblemIndicator<in T> {

    class FromMetric(val metricBuilder: () -> EvalMetric) : ProblemIndicator<Any> {
        // we can't pass metric directly because of cycles in initialization
        private val metric by lazy { metricBuilder() }
    }

    class FromValue<T>(val predicate: (T) -> Boolean) : ProblemIndicator<T>
}

data class EvalMetric(
    val threshold: Double? = null,
    val dependencies: List<Any> = emptyList(),
    val showInCard: Boolean = true
)

object Analysis {
    val HAS_SYNTAX_ERRORS: TrivialEvalData<Boolean> = EvalDataDescription(
        name = "Has syntax errors",
        description = "Bind with `true` if the result has syntax errors",
        problemIndicators = listOf(
            ProblemIndicator.FromMetric { Metrics.WITHOUT_SYNTAX_ERRORS }
        )
    )
}

object Metrics {
    val WITHOUT_SYNTAX_ERRORS: EvalMetric = EvalMetric(
        threshold = 1.0,
        dependencies = listOf(Analysis.HAS_SYNTAX_ERRORS)
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionalType, in, interfaceDeclaration, lambdaLiteral, nestedClass,
nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, propertyDelegate, sealed, stringLiteral,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
