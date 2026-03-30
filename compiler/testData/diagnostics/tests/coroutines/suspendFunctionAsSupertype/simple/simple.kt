// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +SuspendFunctionAsSupertype
// SKIP_TXT

class C: suspend () -> Unit {
    override suspend fun invoke() {
    }
}

fun interface FI: suspend () -> Unit {
}

interface I: suspend () -> Unit {
}

object O: suspend () -> Unit {
    override suspend fun invoke() {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, functionalType, interfaceDeclaration,
objectDeclaration, operator, override, suspend */
