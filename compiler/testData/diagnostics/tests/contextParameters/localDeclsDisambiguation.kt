// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class C

context(x: Any)
fun contextFun() {}

fun test() {
    context(C::class) {
        contextFun()
    }

    context(fun () {} ) {
        contextFun()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext */