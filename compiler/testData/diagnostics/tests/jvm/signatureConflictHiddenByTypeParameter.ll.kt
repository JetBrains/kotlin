// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `ACCIDENTAL_OVERRIDE`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-9152

abstract class A {
    open fun <T> f(x: T): T {
        abstract class B : A() {
            abstract override fun <S> f(x: T): S
        }
        null!!
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, localClass, nullableType, override,
typeParameter */
