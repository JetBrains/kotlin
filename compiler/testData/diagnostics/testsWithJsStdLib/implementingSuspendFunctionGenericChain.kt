// RUN_PIPELINE_TILL: CODEGEN

interface <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>Work<!><T> : suspend (T) -> T

interface <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>StringWork<!> : Work<String>

abstract class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>AbstractWork<!> : StringWork

class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>ConcreteWork<!> : AbstractWork() {
    override suspend fun invoke(value: String): String = value
}
