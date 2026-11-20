// RUN_PIPELINE_TILL: CODEGEN

typealias Work<T> = suspend (T) -> T
typealias StringWork = Work<String>

abstract class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>GenericAlias<!> : Work<String>

class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>ChainedAlias<!> : StringWork {
    override suspend fun invoke(value: String): String = value
}

fun interface Action {
    suspend operator fun invoke()
}

class ActionImplementation : Action {
    override suspend fun invoke() {}
}
