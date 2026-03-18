// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78621

// FILE: WriteAction.java

public interface WriteAction<C> {
    public C compute();
}

// FILE: test.kt

fun <I, O> I.execute(block: I.() -> O): O = block(this)

fun <M> nullableMaterialize(): M? = null

interface PsiElement
interface PsiFile

fun test() {
    WriteAction<PsiFile> {
        nullableMaterialize<PsiElement>()?.execute {
            nullableMaterialize<PsiFile>()
        }
    }
}

/* GENERATED_FIR_TAGS: flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType, interfaceDeclaration,
javaType, lambdaLiteral, nullableType, safeCall, thisExpression, typeParameter, typeWithExtension */
