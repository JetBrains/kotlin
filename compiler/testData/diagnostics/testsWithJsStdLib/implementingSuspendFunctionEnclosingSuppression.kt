// RUN_PIPELINE_TILL: CODEGEN

@Suppress("IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE")
fun suppressed(): Any = object : suspend () -> Unit {
    override suspend fun invoke() {}
}

fun unsuppressed(): Any = <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>object<!> : suspend () -> Unit {
    override suspend fun invoke() {}
}

@Suppress("IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE")
fun suppressedLocalClass(): Any {
    class Local : suspend () -> Unit {
        override suspend fun invoke() {}
    }
    return Local()
}

fun unsuppressedLocalClass(): Any {
    class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>Local<!> : suspend () -> Unit {
        override suspend fun invoke() {}
    }
    return Local()
}
