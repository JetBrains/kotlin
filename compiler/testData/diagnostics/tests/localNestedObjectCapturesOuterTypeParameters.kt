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

        <!WRONG_MODIFIER_TARGET!>sealed<!> <!NESTED_CLASS_NOT_ALLOWED!>class AnalyzeQualifiedElementResult<!> {
            data <!LOCAL_OBJECT_NOT_ALLOWED!>object Skip<!> : AnalyzeQualifiedElementResult()
        }
    }
}

fun short() {
    abstract class ShorteningProcessor<TElement> {
        fun analyzeCollectedElements() = Skip == Skip

        <!LOCAL_OBJECT_NOT_ALLOWED!>object Skip<!>
    }
}
