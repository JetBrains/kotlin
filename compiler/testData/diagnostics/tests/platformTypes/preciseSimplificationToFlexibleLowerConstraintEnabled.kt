// FIR_IDENTICAL
// LANGUAGE: +PreciseSimplificationToFlexibleLowerConstraint
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78621


// FILE: Computable.kt

fun interface Computable<C> {
    fun compute(): C
}

// FILE: WriteAction.java

public class WriteAction {
    public static <R> R compute(Computable<R> action) {
        return action.compute();
    }
}

// FILE: test.kt

fun <I, O> I.execute(block: I.() -> O): O = block(this)

fun <M> nullableMaterialize(): M? = null

interface PsiElement
interface PsiFile

fun test() {
    WriteAction.compute<PsiFile> {
        nullableMaterialize<PsiElement>()?.execute {
            nullableMaterialize<PsiFile>()
        }
    }
}

/* GENERATED_FIR_TAGS: flexibleType, javaFunction, lambdaLiteral, nullableType, safeCall, typeParameter */
