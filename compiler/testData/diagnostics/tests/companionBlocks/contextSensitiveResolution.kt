// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions +ContextSensitiveResolutionUsingExpectedType

class C {
    companion {
        val Instance get() = C()
    }
}

fun test(): C {
    return Instance
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, propertyDeclaration */
