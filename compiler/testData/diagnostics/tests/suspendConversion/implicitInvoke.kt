// RUN_PIPELINE_TILL: BACKEND
fun box() = useSuspendFunInt(Test())

fun useSuspendFunInt(fn: suspend () -> String): String = ""

open class Test : () -> String {
    override fun invoke() = "OK"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, operator, override, stringLiteral, suspend */
