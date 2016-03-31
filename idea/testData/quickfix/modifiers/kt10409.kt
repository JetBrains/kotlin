// "Make 'IterablePipeline' abstract" "true"
// ERROR: 'pipe' overrides nothing
// ERROR: One type argument expected for interface Pipeline<TPipeline> defined in root package

// Actually this test is about getting rid of assertion happenning while creating quick fixes
// See KT-10409
interface Pipeline<TPipeline> {
    fun pipe(block: Pipeline<TPipeline, String>)
}

<caret>class IterablePipeline<T> : Pipeline<T> {
    override fun pipe(block: Pipeline<T>) {
    }
}
