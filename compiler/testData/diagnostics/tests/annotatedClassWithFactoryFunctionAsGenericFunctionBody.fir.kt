// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78985

annotation class Anno

fun <T> genericFun(): T =

@Anno
class Annotated {
    companion object {
        fun getAnnotated(): Annotated = Annotated()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, nullableType, typeParameter */
