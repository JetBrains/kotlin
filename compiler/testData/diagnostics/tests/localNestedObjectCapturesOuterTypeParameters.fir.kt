// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76806

fun full() {
    abstract class ShorteningProcessor<TElement> {
        fun analyzeCollectedElements(): Int {
            val result = analyzeQualifiedElement()

            return when (result) {
                AnalyzeQualifiedElementResult.Skip -> 1
            }
        }

        abstract fun analyzeQualifiedElement(): AnalyzeQualifiedElementResult

        sealed class AnalyzeQualifiedElementResult {
            data object Skip : AnalyzeQualifiedElementResult()
        }
    }
}

fun short() {
    abstract class ShorteningProcessor<TElement> {
        fun analyzeCollectedElements() = Skip == Skip

        object Skip
    }
}
