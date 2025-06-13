// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

val <T> T.f1: context(T) (T) -> T
    get() = {
        contextOf<T>()
    }

fun usage() {
    with(1) {
        with(2) {
            f1(3)
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, getter, integerLiteral, lambdaLiteral, nullableType,
propertyDeclaration, propertyWithExtensionReceiver, typeParameter, typeWithContext */
