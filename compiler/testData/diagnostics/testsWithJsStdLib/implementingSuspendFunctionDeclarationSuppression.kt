// RUN_PIPELINE_TILL: CODEGEN

@Suppress("IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE")
class Suppressed : suspend () -> Unit {
    override suspend fun invoke() {}
}

class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>Unsuppressed<!> : suspend () -> Unit {
    override suspend fun invoke() {}
}

class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>Derived<!> : SuppressedInterface {
    override suspend fun invoke() {}
}

@Suppress("IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE")
interface SuppressedInterface : suspend () -> Unit
