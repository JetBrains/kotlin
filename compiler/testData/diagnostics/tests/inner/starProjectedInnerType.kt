// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-63444
class A<D : Any>  {
    inner class Ainner<DD : D?> {
        fun innerFun() {}
    }

    fun test(w:Ainner<*>) {
        w.innerFun()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, nullableType, starProjection, typeConstraint,
typeParameter */
