// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

class Box<T> {
    lateinit val prop: T
}

class Out<out T> {
    lateinit val prop: <!TYPE_VARIANCE_CONFLICT_ERROR!>T<!>
}

class In<in T> {
    lateinit val prop: <!TYPE_VARIANCE_CONFLICT_ERROR!>T<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, lateinit, propertyDeclaration */
