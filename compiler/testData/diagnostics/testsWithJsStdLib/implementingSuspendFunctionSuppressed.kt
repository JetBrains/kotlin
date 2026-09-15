// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:Suppress("IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE")

@Suppress("IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE")
class DeclarationSuppressed : suspend () -> Unit {
    override suspend fun invoke() {}
}

@Suppress("IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE")
fun enclosingDeclarationSuppressed(): Any = object : suspend () -> Unit {
    override suspend fun invoke() {}
}

object FileSuppressed : suspend () -> Unit {
    override suspend fun invoke() {}
}