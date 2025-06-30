// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun <R> inlineFunOnlyLocal(crossinline p: () -> R) {
    <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> A {

        val z = p()

        <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> a() {
            p()
        }
    }
}

inline fun <R> inlineFun(p: () -> R) {
    <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> A {

        val z = <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()

        <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> a() {
            <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, crossinline, functionDeclaration, functionalType, inline, localClass,
nullableType, propertyDeclaration, typeParameter */
