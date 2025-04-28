// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77149

fun foo() {
    abstract class ShorteningProcessor<TElement> {
        fun analyzeCollectedElements() = E.A == E.A

        enum class E {
            A;
        }
    }
}
