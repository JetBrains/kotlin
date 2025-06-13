// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT

class A {
    lateinit var m: Map<String, Int>

    @ExperimentalStdlibApi
    fun foo(xs: Collection<List<String>>) {
        m = buildMap {
            // flatMap calls might be completed on early phase
            for (x in xs.flatMap { it.toList() }) {
                put(x, x.length)
            }
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, forLoop, functionDeclaration, lambdaLiteral, lateinit,
localProperty, nullableType, propertyDeclaration */
