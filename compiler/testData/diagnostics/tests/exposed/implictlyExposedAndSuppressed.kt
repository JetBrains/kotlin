// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: -ReportExposedTypeForMoreCasesOfTypeParameterBounds, -ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType

// MODULE: a

internal interface Inter {
    fun foo() = 10
}

class Wrapper<T>(val it: T)

fun <T: Inter?> public(a: T & Any) = Wrapper(a)

@Suppress("EXPOSED_FUNCTION_RETURN_TYPE")
fun other() = public(object : Inter {})
